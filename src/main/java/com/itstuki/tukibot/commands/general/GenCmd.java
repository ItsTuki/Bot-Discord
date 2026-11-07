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
import com.itstuki.tukibot.fun.BotLogic;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/** !gen del bot original. Ahora la contraseña solo la ve quien la pidió. */
public class GenCmd extends SlashCommand
{
    public GenCmd(Bot bot)
    {
        super(bot);
        this.name = "gen";
        this.nameEs = "gen";
        this.help = "Genera una contraseña aleatoria (solo tú la ves)";
        this.helpEn = "Generates a random password (only you can see it)";
        this.guildOnly = false;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addOptions(Loc.option(OptionType.INTEGER, "length", "largo", "Cantidad de caracteres (por defecto 10)", "Number of characters", false)
                .setRequiredRange(4, 64));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        int length = (int) ctx.integer("length", 10);
        ctx.reply("🔐 La clave generada es: ||`" + BotLogic.genPass(length) + "`||", true);
    }
}
