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
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.music.PlayerActions;

public class ForceskipCmd extends SlashCommand
{
    public ForceskipCmd(Bot bot)
    {
        super(bot);
        this.name = "forceskip";
        this.nameEs = "forzar-salto";
        this.help = "Salta la canción actual sin votación";
        this.helpEn = "Skips the current song";
        this.category = Category.DJ;
        this.bePlaying = true;
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        ctx.reply(PlayerActions.forceSkip(bot, ctx.handler()));
    }
}
