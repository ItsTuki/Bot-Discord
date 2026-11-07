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

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Canción de Spotify que, al momento de sonar, se busca en YouTube Music
 * (y si no aparece, en YouTube) y se reproduce desde ahí. Se elige el
 * resultado con la duración más parecida a la de Spotify.
 */
public class SpotifyMirrorTrack extends DelegatedAudioTrack
{
    private final SpotifySourceManager sourceManager;

    public SpotifyMirrorTrack(AudioTrackInfo info, SpotifySourceManager sourceManager)
    {
        super(info);
        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception
    {
        AudioTrack match = sourceManager.findMatch(this);
        AudioTrack playable = sourceManager.getPlayerManager().prepare(match.makeClone());
        processDelegate((InternalAudioTrack) playable, executor);
    }

    /** Busca la canción en YouTube (sin caché; ver SpotifySourceManager#findMatch). */
    AudioTrack searchMatch() throws Exception
    {
        String query = (trackInfo.author == null || trackInfo.author.isBlank() ? "" : trackInfo.author.split(",")[0].trim() + " - ") + trackInfo.title;
        AudioTrack match = find("ytmsearch:", query);
        if(match == null)
            match = find("ytsearch:", query);
        if(match == null)
            throw new FriendlyException("No encontré \"" + query + "\" en YouTube.", FriendlyException.Severity.COMMON, null);
        return match;
    }

    private AudioTrack find(String prefix, String query) throws Exception
    {
        List<AudioTrack> results = sourceManager.getPlayerManager().quickSearch(prefix, query).get(20, TimeUnit.SECONDS);
        AudioTrack best = null;
        long bestDiff = Long.MAX_VALUE;
        for(int i = 0; i < Math.min(5, results.size()); i++)
        {
            AudioTrack t = results.get(i);
            if(t.getInfo().isStream)
                continue;
            long diff = trackInfo.length > 0 ? Math.abs(t.getDuration() - trackInfo.length) : i;
            // se prefiere el orden de YouTube salvo que la duración sea muy distinta
            if(diff + i * 3000L < bestDiff)
            {
                bestDiff = diff + i * 3000L;
                best = t;
            }
        }
        return best == null ? null : best.makeClone();
    }

    @Override
    protected AudioTrack makeShallowClone()
    {
        return new SpotifyMirrorTrack(trackInfo, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager()
    {
        return sourceManager;
    }
}
