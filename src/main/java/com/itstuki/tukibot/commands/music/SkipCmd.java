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
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.music.PlayerActions;

public class SkipCmd extends SlashCommand
{
    public SkipCmd(Bot bot)
    {
        super(bot);
        this.name = "skip";
        this.nameEs = "saltar";
        this.help = "Vota para saltar la canción actual (si la pediste tú, se salta de inmediato)";
        this.helpEn = "Votes to skip the current song";
        this.category = Category.MUSIC;
        this.beListening = true;
        this.bePlaying = true;
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        ctx.reply(PlayerActions.voteSkip(bot, ctx.member(), ctx.handler()));
    }
}
