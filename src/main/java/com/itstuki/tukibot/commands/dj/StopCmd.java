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

public class StopCmd extends SlashCommand
{
    public StopCmd(Bot bot)
    {
        super(bot);
        this.name = "stop";
        this.nameEs = "detener";
        this.help = "Detiene la música, vacía la cola y sale del canal de voz";
        this.helpEn = "Stops the music, clears the queue and leaves";
        this.category = Category.DJ;
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        ctx.reply(PlayerActions.stop(bot, ctx.guild(), ctx.handler()));
    }
}
