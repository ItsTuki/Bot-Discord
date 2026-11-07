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
import com.itstuki.tukibot.settings.RepeatMode;
import com.itstuki.tukibot.settings.Settings;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class RepeatCmd extends SlashCommand
{
    public RepeatCmd(Bot bot)
    {
        super(bot);
        this.name = "repeat";
        this.nameEs = "repetir";
        this.help = "Repite la cola o una canción al terminar";
        this.helpEn = "Re-adds music to the queue when finished";
        this.category = Category.DJ;
        this.musicChecks = false; // como en JMusicBot: no importa dónde se use
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addOptions(Loc.option(OptionType.STRING, "mode", "modo", "Modo de repetición (vacío = alternar)", "Repeat mode (empty = toggle)", false)
                .addChoice("Desactivada", "OFF").addChoice("Toda la cola", "ALL").addChoice("Una canción", "SINGLE"));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        Settings settings = ctx.settings();
        String mode = ctx.string("mode", null);
        RepeatMode value = mode == null
                ? (settings.getRepeatMode() == RepeatMode.OFF ? RepeatMode.ALL : RepeatMode.OFF)
                : RepeatMode.valueOf(mode);
        settings.setRepeatMode(value);
        bot.getNowplayingHandler().refresh(ctx.guild());
        ctx.replySuccess("Repetición: **" + value.getUserFriendlyName() + "** " + (value.getEmoji() == null ? "" : value.getEmoji()));
    }
}
