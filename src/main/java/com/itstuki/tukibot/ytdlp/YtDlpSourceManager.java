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
package com.itstuki.tukibot.ytdlp;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * "Fuente" de las pistas reproducidas con yt-dlp. No carga búsquedas ni
 * enlaces (eso lo sigue haciendo youtube-source); solo reproduce. Se llama
 * "youtube" para que el resto del bot las trate como pistas de YouTube.
 */
public class YtDlpSourceManager implements AudioSourceManager
{
    private final YtDlp ytdlp;
    private final HttpInterfaceManager httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();

    public YtDlpSourceManager(YtDlp ytdlp)
    {
        this.ytdlp = ytdlp;
    }

    public YtDlp getYtDlp()
    {
        return ytdlp;
    }

    public HttpInterface getHttpInterface()
    {
        return httpInterfaceManager.getInterface();
    }

    /** Envuelve una pista de YouTube para reproducirla con yt-dlp. */
    public AudioTrack wrap(AudioTrack youtubeTrack)
    {
        return new YtDlpAudioTrack(youtubeTrack.getInfo(), this, youtubeTrack);
    }

    @Override
    public String getSourceName()
    {
        return "youtube";
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference)
    {
        return null;
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track)
    {
        return false;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown()
    {
        try
        {
            httpInterfaceManager.close();
        }
        catch(IOException ignored) {}
    }
}
