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
import com.itstuki.tukibot.ui.Panels;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class QueueCmd extends SlashCommand
{
    public QueueCmd(Bot bot)
    {
        super(bot);
        this.name = "queue";
        this.nameEs = "cola";
        this.help = "Muestra la cola de reproducción";
        this.helpEn = "Shows the current queue";
        this.category = Category.MUSIC;
        this.bePlaying = true;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addOptions(Loc.option(OptionType.INTEGER, "page", "pagina", "Número de página", "Page number", false).setMinValue(1));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        ctx.reply(Panels.queue(bot, ctx.guild(), ctx.handler(), (int) ctx.integer("page", 1)), false, null);
    }
}
