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
package com.itstuki.tukibot.commands.general;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.commands.*;

public class PingCmd extends SlashCommand
{
    public PingCmd(Bot bot)
    {
        super(bot);
        this.name = "ping";
        this.nameEs = "ping";
        this.help = "Revisa la latencia del bot";
        this.helpEn = "Checks the bot's latency";
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        long created = ctx.event().getTimeCreated().toInstant().toEpochMilli();
        long gateway = ctx.event().getJDA().getGatewayPing();
        ctx.event().reply("🏓 Pong!").queue(hook -> hook.retrieveOriginal().queue(m -> {
            long rtt = m.getTimeCreated().toInstant().toEpochMilli() - created;
            hook.editOriginal("🏓 Pong! Respuesta: `" + rtt + " ms` · Gateway: `" + gateway + " ms`").queue();
        }));
    }
}
