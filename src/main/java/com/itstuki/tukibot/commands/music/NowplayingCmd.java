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
package com.itstuki.tukibot.commands.music;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.audio.AudioHandler;
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.ui.Panels;

public class NowplayingCmd extends SlashCommand
{
    public NowplayingCmd(Bot bot)
    {
        super(bot);
        this.name = "nowplaying";
        this.nameEs = "sonando";
        this.help = "Muestra el panel de la canción que está sonando, con botones de control";
        this.helpEn = "Shows the now playing panel with control buttons";
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        AudioHandler handler = ctx.handler();
        if(!handler.isMusicPlaying(ctx.guild().getJDA()))
        {
            ctx.reply(Panels.noMusic(bot, ctx.guild(), handler), false, null);
            bot.getNowplayingHandler().clearLastNPMessage(ctx.guild());
            return;
        }
        ctx.reply(Panels.nowPlaying(bot, ctx.guild(), handler), false, msg -> bot.getNowplayingHandler().setLastNPMessage(msg));
    }
}
