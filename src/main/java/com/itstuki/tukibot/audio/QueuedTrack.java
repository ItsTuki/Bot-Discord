/*
 * Copyright 2021 John Grosh <john.a.grosh@gmail.com>.
 * Copyright 2026 ItsTuki (migración a JDA 6, DAVE y comandos slash).
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

import com.itstuki.tukibot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.itstuki.tukibot.utils.FormatUtil;
import com.itstuki.tukibot.queue.Queueable;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class QueuedTrack implements Queueable
{
    private final AudioTrack track;
    private final RequestMetadata requestMetadata;

    public QueuedTrack(AudioTrack track, RequestMetadata rm)
    {
        this.track = track;
        this.track.setUserData(rm == null ? RequestMetadata.EMPTY : rm);

        this.requestMetadata = rm == null ? RequestMetadata.EMPTY : rm;
        if (this.track.isSeekable() && rm != null && rm.requestInfo != null && rm.requestInfo.startTimestamp > 0)
            track.setPosition(rm.requestInfo.startTimestamp);
    }
    
    @Override
    public long getIdentifier() 
    {
        return requestMetadata.getOwner();
    }
    
    public AudioTrack getTrack()
    {
        return track;
    }

    public RequestMetadata getRequestMetadata()
    {
        return requestMetadata;
    }

    @Override
    public String toString()
    {
        long owner = requestMetadata.getOwner();
        return "`[" + TimeUtil.formatTime(track.getDuration()) + "]` " + FormatUtil.trackLink(track)
                + (owner == 0L ? "" : " - <@" + owner + ">");
    }
}
