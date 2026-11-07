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

public class PauseCmd extends SlashCommand
{
    public PauseCmd(Bot bot)
    {
        super(bot);
        this.name = "pause";
        this.nameEs = "pausar";
        this.help = "Pausa la canción actual";
        this.helpEn = "Pauses the current song";
        this.category = Category.DJ;
        this.bePlaying = true;
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        AudioHandler handler = ctx.handler();
        if(handler.getPlayer().isPaused())
        {
            ctx.replyWarning("La música ya está en pausa. Usa " + bot.getCommands().mention("resume") + " para seguir.");
            return;
        }
        handler.getPlayer().setPaused(true);
        bot.getNowplayingHandler().refresh(ctx.guild());
        ctx.replySuccess("Pausé **" + FormatUtil.escape(handler.getPlayer().getPlayingTrack().getInfo().title) + "**. Usa "
                + bot.getCommands().mention("resume") + " para seguir.");
    }
}
