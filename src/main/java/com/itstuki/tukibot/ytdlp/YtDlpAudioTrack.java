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

import com.sedmelluq.discord.lavaplayer.container.matroska.MatroskaAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pista de YouTube que obtiene el audio con yt-dlp. Si yt-dlp falla antes de
 * empezar a sonar, usa la pista original de youtube-source como respaldo.
 */
public class YtDlpAudioTrack extends DelegatedAudioTrack
{
    private static final Logger LOG = LoggerFactory.getLogger("yt-dlp");

    private final YtDlpSourceManager sourceManager;
    private final AudioTrack fallback;

    public YtDlpAudioTrack(AudioTrackInfo info, YtDlpSourceManager sourceManager, AudioTrack fallback)
    {
        super(info);
        this.sourceManager = sourceManager;
        this.fallback = fallback;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception
    {
        YtDlpDownload download;
        try
        {
            download = sourceManager.getYtDlp().takePrefetched(trackInfo.uri);
            if(download == null)
                download = sourceManager.getYtDlp().download(trackInfo.uri);
            download.awaitStart(60_000);
        }
        catch(Exception e)
        {
            LOG.warn("yt-dlp no pudo obtener " + trackInfo.identifier + ": " + e.getMessage()
                    + (fallback != null ? " — probando con el método interno." : ""));
            if(fallback == null)
                throw new FriendlyException("yt-dlp: " + e.getMessage(), FriendlyException.Severity.COMMON, e);
            try
            {
                processDelegate((InternalAudioTrack) fallback.makeClone(), executor);
            }
            catch(Exception fallbackError)
            {
                // se muestra el error de yt-dlp, que suele explicar mejor qué pasó
                throw new FriendlyException("YouTube no entregó el audio. yt-dlp dijo: " + shortError(e.getMessage()),
                        FriendlyException.Severity.COMMON, fallbackError);
            }
            return;
        }

        try(YtDlpDownload active = download)
        {
            String ext = download.getExt();
            InternalAudioTrack delegate = "webm".equalsIgnoreCase(ext) || "weba".equalsIgnoreCase(ext) || "mkv".equalsIgnoreCase(ext)
                    ? new MatroskaAudioTrack(trackInfo, download)
                    : new MpegAudioTrack(trackInfo, download);
            processDelegate(delegate, executor);
        }
    }

    private static String shortError(String msg)
    {
        if(msg == null)
            return "error desconocido";
        String m = msg.replaceFirst("^ERROR: \\[youtube\\] [\\w-]+: ", "");
        int cut = m.indexOf(". Use --cookies");
        if(cut > 0)
            m = m.substring(0, cut + 1);
        return m.length() > 300 ? m.substring(0, 299) + "…" : m;
    }

    @Override
    protected AudioTrack makeShallowClone()
    {
        return new YtDlpAudioTrack(trackInfo, sourceManager, fallback == null ? null : fallback.makeClone());
    }

    @Override
    public AudioSourceManager getSourceManager()
    {
        return sourceManager;
    }
}
