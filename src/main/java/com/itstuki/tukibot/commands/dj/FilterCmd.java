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
import com.itstuki.tukibot.audio.AudioFilter;
import com.itstuki.tukibot.audio.AudioHandler;
import com.itstuki.tukibot.commands.*;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class FilterCmd extends SlashCommand
{
    public FilterCmd(Bot bot)
    {
        super(bot);
        this.name = "filter";
        this.nameEs = "filtro";
        this.help = "Aplica un efecto al audio (bass boost, nightcore, 8D...)";
        this.helpEn = "Applies an audio effect (bass boost, nightcore, 8D...)";
        this.category = Category.DJ;
        this.bePlaying = true;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        OptionData opt = Loc.option(OptionType.STRING, "effect", "efecto", "Efecto a aplicar", "Effect to apply", true);
        for(AudioFilter f : AudioFilter.values())
            opt.addChoice(f.getEmoji() + " " + f.getLabel(), f.name());
        data.addOptions(opt);
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        AudioHandler handler = ctx.handler();
        AudioFilter filter = AudioFilter.valueOf(ctx.string("effect", "OFF"));
        handler.setFilter(filter);
        bot.getNowplayingHandler().refresh(ctx.guild());
        if(filter == AudioFilter.OFF)
            ctx.replySuccess("Filtros quitados; el audio vuelve a la normalidad.");
        else
            ctx.replySuccess("Filtro **" + filter.getEmoji() + " " + filter.getLabel() + "** aplicado."
                    + "\n-# Tarda un par de segundos en notarse. Quítalo con `/filter Sin filtro`.");
    }
}
