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
package com.itstuki.tukibot.commands.dj;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.audio.AudioHandler;
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.utils.FormatUtil;

/** Nuevo: en JMusicBot se reanudaba con "play" sin argumentos. */
public class ResumeCmd extends SlashCommand
{
    public ResumeCmd(Bot bot)
    {
        super(bot);
        this.name = "resume";
        this.nameEs = "reanudar";
        this.help = "Reanuda la música pausada";
        this.helpEn = "Resumes the paused song";
        this.category = Category.DJ;
        this.bePlaying = true;
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        AudioHandler handler = ctx.handler();
        if(!handler.getPlayer().isPaused())
        {
            ctx.replyWarning("La música no está en pausa.");
            return;
        }
        handler.getPlayer().setPaused(false);
        bot.getNowplayingHandler().refresh(ctx.guild());
        ctx.replySuccess("Reanudé **" + FormatUtil.escape(handler.getPlayer().getPlayingTrack().getInfo().title) + "**.");
    }
}
