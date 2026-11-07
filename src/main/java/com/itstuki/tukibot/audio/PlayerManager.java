/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 * Copyright 2026 ItsTuki (youtube-source actual, OAuth y búsquedas para autocompletado).
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
package com.itstuki.tukibot.audio;

import com.dunctebot.sourcemanagers.DuncteBotSources;
import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.BotConfig;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.itstuki.tukibot.spotify.SpotifySourceManager;
import com.itstuki.tukibot.ytdlp.YtDlp;
import com.itstuki.tukibot.ytdlp.YtDlpSourceManager;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.YoutubeSourceOptions;
import dev.lavalink.youtube.clients.*;
import dev.lavalink.youtube.clients.skeleton.Client;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Administrador de reproductores: registra las fuentes de audio y crea un
 * {@link AudioHandler} por servidor.
 */
public class PlayerManager extends DefaultAudioPlayerManager
{
    private static final Logger LOG = LoggerFactory.getLogger("PlayerManager");
    private static final long SEARCH_CACHE_MILLIS = 5 * 60 * 1000;

    private final Bot bot;
    private final Map<String, CachedSearch> searchCache = new ConcurrentHashMap<>();
    private YoutubeAudioSourceManager youtube;
    private YtDlpSourceManager ytdlp;

    public PlayerManager(Bot bot)
    {
        this.bot = bot;
    }

    public void init()
    {
        BotConfig config = bot.getConfig();
        TransformativeAudioSourceManager.createTransforms(config.getTransforms()).forEach(this::registerSourceManager);

        registerSourceManager(new SpotifySourceManager(this));

        youtube = createYoutubeSource(config);
        registerSourceManager(youtube);

        YtDlp ytdlpTool = new YtDlp(config);
        ytdlp = new YtDlpSourceManager(ytdlpTool);
        ytdlpTool.init(bot.getThreadpool());

        registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        registerSourceManager(new BandcampAudioSourceManager());
        registerSourceManager(new VimeoAudioSourceManager());
        registerSourceManager(new TwitchStreamAudioSourceManager());
        registerSourceManager(new BeamAudioSourceManager());
        registerSourceManager(new GetyarnAudioSourceManager());
        registerSourceManager(new NicoAudioSourceManager());
        registerSourceManager(new HttpAudioSourceManager(MediaContainerRegistry.DEFAULT_REGISTRY));

        AudioSourceManagers.registerLocalSource(this);

        DuncteBotSources.registerAll(this, "es-ES");

        // si se activó OAuth sin token, se guarda solo cuando el usuario vincule la cuenta
        if(config.useYoutubeOauth() && config.getYoutubeRefreshToken() == null)
        {
            java.util.concurrent.ScheduledFuture<?>[] task = new java.util.concurrent.ScheduledFuture<?>[1];
            task[0] = bot.getThreadpool().scheduleWithFixedDelay(() -> {
                String token = youtube.getOauth2RefreshToken();
                if(token != null && !token.isBlank())
                {
                    if(config.saveYoutubeRefreshToken(token))
                        LOG.info("Cuenta de YouTube vinculada. Guardé el refresh token en config.txt.");
                    task[0].cancel(false);
                }
            }, 15, 15, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    private static YoutubeAudioSourceManager createYoutubeSource(BotConfig config)
    {
        // Orden pensado según cómo responde YouTube en 2026: MWEB y WEB necesitan
        // descifrar firmas (lo hace el servidor remoto), ANDROID_VR a veces pide
        // iniciar sesión. Los clientes de TV (OAuth) deben ir DESPUÉS de uno web,
        // porque reutilizan el cifrado que este obtiene.
        List<Client> clients = new ArrayList<>();
        clients.add(new MusicWithThumbnail());
        clients.add(new MWebWithThumbnail());
        clients.add(new WebWithThumbnail());
        if(config.useYoutubeOauth())
        {
            clients.add(new TvHtml5SimplyWithThumbnail());
            clients.add(new Tv());
        }
        clients.add(new AndroidVrWithThumbnail());
        clients.add(new WebEmbeddedWithThumbnail());

        YoutubeSourceOptions options = new YoutubeSourceOptions()
                .setAllowSearch(true)
                .setAllowDirectVideoIds(true)
                .setAllowDirectPlaylistIds(true);
        if(config.getYoutubeRemoteCipher() != null)
        {
            options.setRemoteCipher(config.getYoutubeRemoteCipher(), config.getYoutubeRemoteCipherPassword(), "TukiBot");
            LOG.info("YouTube: descifrando firmas con " + config.getYoutubeRemoteCipher());
        }

        YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager(options, clients.toArray(new Client[0]));
        yt.setPlaylistPageCount(config.getMaxYTPlaylistPages());
        if(config.useYoutubeOauth())
        {
            yt.useOauth2(config.getYoutubeRefreshToken(), false);
            if(config.getYoutubeRefreshToken() == null)
                LOG.warn("OAuth de YouTube activado sin refresh token: sigue las instrucciones que aparecerán en la consola "
                        + "y luego copia el refresh token en config.txt (youtube.refreshtoken).");
        }
        return yt;
    }

    public Bot getBot()
    {
        return bot;
    }

    public YoutubeAudioSourceManager getYoutube()
    {
        return youtube;
    }

    /**
     * Prepara una pista antes de reproducirla: las de YouTube pasan a
     * reproducirse con yt-dlp (si está listo), con youtube-source de respaldo.
     */
    public AudioTrack prepare(AudioTrack track)
    {
        if(track != null && ytdlp != null && ytdlp.getYtDlp().isReady() && track.getSourceManager() == youtube)
        {
            AudioTrack wrapped = ytdlp.wrap(track);
            wrapped.setUserData(track.getUserData());
            if(track.getPosition() > 0)
                wrapped.setPosition(track.getPosition());
            return wrapped;
        }
        return track;
    }

    public boolean hasHandler(Guild guild)
    {
        return guild.getAudioManager().getSendingHandler()!=null;
    }

    /** Devuelve el handler del servidor si existe, sin crearlo. */
    public AudioHandler getHandler(Guild guild)
    {
        return (AudioHandler) guild.getAudioManager().getSendingHandler();
    }

    public synchronized AudioHandler setUpHandler(Guild guild)
    {
        AudioHandler handler;
        if(guild.getAudioManager().getSendingHandler()==null)
        {
            AudioPlayer player = createPlayer();
            player.setVolume(bot.getSettingsManager().getSettings(guild).getVolume());
            handler = new AudioHandler(this, guild, player);
            player.addListener(handler);
            guild.getAudioManager().setSendingHandler(handler);
            guild.getAudioManager().setSelfDeafened(true);
        }
        else
            handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        return handler;
    }

    /**
     * Búsqueda rápida para el autocompletado de Discord (que exige responder
     * en menos de 3 segundos). Guarda los resultados unos minutos para no
     * repetir búsquedas mientras la persona escribe.
     */
    public CompletableFuture<List<AudioTrack>> quickSearch(String prefix, String query)
    {
        String key = prefix + query.toLowerCase(Locale.ROOT);
        CachedSearch cached = searchCache.get(key);
        if(cached != null && cached.expires > System.currentTimeMillis())
            return CompletableFuture.completedFuture(cached.tracks);
        if(searchCache.size() > 500)
            searchCache.entrySet().removeIf(e -> e.getValue().expires < System.currentTimeMillis());

        CompletableFuture<List<AudioTrack>> future = new CompletableFuture<>();
        loadItem(prefix + query, new AudioLoadResultHandler()
        {
            @Override
            public void trackLoaded(AudioTrack track)
            {
                future.complete(List.of(track));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist)
            {
                future.complete(playlist.getTracks());
            }

            @Override
            public void noMatches()
            {
                future.complete(List.of());
            }

            @Override
            public void loadFailed(FriendlyException exception)
            {
                future.complete(List.of());
            }
        });
        return future.thenApply(list -> {
            List<AudioTrack> copy = List.copyOf(list.subList(0, Math.min(25, list.size())));
            if(!copy.isEmpty())
                searchCache.put(key, new CachedSearch(copy, System.currentTimeMillis() + SEARCH_CACHE_MILLIS));
            return copy;
        });
    }

    private record CachedSearch(List<AudioTrack> tracks, long expires) {}
}
