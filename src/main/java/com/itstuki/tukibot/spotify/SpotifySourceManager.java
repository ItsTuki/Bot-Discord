/*
 * Copyright 2026 ItsTuki.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itstuki.tukibot.spotify;

import com.itstuki.tukibot.audio.PlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Enlaces de Spotify (canciones, álbumes y playlists). Lee los datos de la
 * página pública de "embed" de Spotify, sin claves de API, y cada canción se
 * busca en YouTube Music cuando le toca sonar ({@link SpotifyMirrorTrack}).
 */
public class SpotifySourceManager implements AudioSourceManager
{
    private static final Pattern URL = Pattern.compile(
            "^(?:https?://)?(?:open|play)\\.spotify\\.com/(?:intl-[a-zA-Z-]+/)?(?:embed/)?(track|album|playlist)/([A-Za-z0-9]{10,})(?:[/?#].*)?$");
    private static final Pattern URI_PATTERN = Pattern.compile("^spotify:(track|album|playlist):([A-Za-z0-9]{10,})$");
    private static final Pattern NEXT_DATA = Pattern.compile("<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>", Pattern.DOTALL);

    private final PlayerManager playerManager;
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .proxy(ProxySelector.getDefault())
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public SpotifySourceManager(PlayerManager playerManager)
    {
        this.playerManager = playerManager;
    }

    public PlayerManager getPlayerManager()
    {
        return playerManager;
    }

    @Override
    public String getSourceName()
    {
        return "spotify";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference)
    {
        String id = reference.identifier.trim();
        Matcher m = URL.matcher(id);
        if(!m.matches())
        {
            m = URI_PATTERN.matcher(id);
            if(!m.matches())
                return null;
        }
        String type = m.group(1);
        String spotifyId = m.group(2);
        try
        {
            JSONObject entity = fetchEntity(type, spotifyId);
            String cover = cover(entity);
            if(type.equals("track"))
                return track(entity.optString("name", entity.optString("title")), artists(entity), entity.optLong("duration"),
                        spotifyId, cover);

            JSONArray list = entity.optJSONArray("trackList");
            List<AudioTrack> tracks = new ArrayList<>();
            if(list != null)
                for(int i = 0; i < list.length(); i++)
                {
                    JSONObject t = list.getJSONObject(i);
                    if(!t.optBoolean("isPlayable", true))
                        continue;
                    String trackId = t.optString("uri", "").replace("spotify:track:", "");
                    String artist = t.optString("subtitle", "").replace(' ', ' ');
                    tracks.add(track(t.optString("title"), artist, t.optLong("duration"), trackId, cover));
                }
            if(tracks.isEmpty())
                return AudioReference.NO_TRACK;
            return new BasicAudioPlaylist(entity.optString("name", "Spotify"), tracks, null, false);
        }
        catch(FriendlyException e)
        {
            throw e;
        }
        catch(IOException | InterruptedException e)
        {
            throw new FriendlyException("No se pudo leer el enlace de Spotify.", FriendlyException.Severity.COMMON, e);
        }
        catch(RuntimeException e)
        {
            throw new FriendlyException("Spotify cambió su página y no pude leer el enlace.", FriendlyException.Severity.SUSPICIOUS, e);
        }
    }

    private AudioTrack track(String title, String artist, long duration, String id, String cover)
    {
        AudioTrackInfo info = new AudioTrackInfo(title, artist, duration, id, false,
                "https://open.spotify.com/track/" + id, cover, null);
        return new SpotifyMirrorTrack(info, this);
    }

    private JSONObject fetchEntity(String type, String id) throws IOException, InterruptedException
    {
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://open.spotify.com/embed/" + type + "/" + id))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36")
                .header("Accept-Language", "es-CL,es;q=0.9,en;q=0.8")
                .timeout(Duration.ofSeconds(15))
                .GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if(res.statusCode() == 404)
            throw new FriendlyException("Ese enlace de Spotify no existe o es privado.", FriendlyException.Severity.COMMON, null);
        if(res.statusCode() != 200)
            throw new IOException("Spotify respondió HTTP " + res.statusCode());
        Matcher m = NEXT_DATA.matcher(res.body());
        if(!m.find())
            throw new IllegalStateException("No se encontró __NEXT_DATA__");
        return new JSONObject(m.group(1)).getJSONObject("props").getJSONObject("pageProps")
                .getJSONObject("state").getJSONObject("data").getJSONObject("entity");
    }

    private static String artists(JSONObject entity)
    {
        JSONArray arr = entity.optJSONArray("artists");
        if(arr == null)
            return entity.optString("subtitle", "");
        List<String> names = new ArrayList<>();
        for(int i = 0; i < arr.length(); i++)
            names.add(arr.getJSONObject(i).optString("name"));
        return String.join(", ", names);
    }

    private static String cover(JSONObject entity)
    {
        JSONObject vi = entity.optJSONObject("visualIdentity");
        JSONArray images = vi == null ? null : vi.optJSONArray("image");
        if(images == null)
        {
            JSONObject coverArt = entity.optJSONObject("coverArt");
            images = coverArt == null ? null : coverArt.optJSONArray("sources");
        }
        if(images == null || images.isEmpty())
            return null;
        // la imagen más grande
        JSONObject best = images.getJSONObject(0);
        for(int i = 1; i < images.length(); i++)
            if(images.getJSONObject(i).optInt("maxWidth", images.getJSONObject(i).optInt("width")) >
                    best.optInt("maxWidth", best.optInt("width")))
                best = images.getJSONObject(i);
        return best.optString("url", null);
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track)
    {
        return false;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() {}
}
