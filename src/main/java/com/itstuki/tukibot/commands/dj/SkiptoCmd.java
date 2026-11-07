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
import com.itstuki.tukibot.music.Autocomplete;
import com.itstuki.tukibot.utils.FormatUtil;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class SkiptoCmd extends SlashCommand
{
    public SkiptoCmd(Bot bot)
    {
        super(bot);
        this.name = "skipto";
        this.nameEs = "saltar-a";
        this.help = "Salta directo a una canción de la cola";
        this.helpEn = "Skips to the specified song";
        this.category = Category.DJ;
        this.bePlaying = true;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addOptions(Loc.option(OptionType.INTEGER, "position", "posicion", "Posición en la cola", "Queue position", true)
                .setMinValue(1).setAutoComplete(true));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        int index = (int) ctx.integer("position", 0);
        AudioHandler handler = ctx.handler();
        if(index < 1 || index > handler.getQueue().size())
        {
            ctx.replyError("La posición tiene que estar entre 1 y " + handler.getQueue().size() + ".");
            return;
        }
        handler.getQueue().skip(index - 1);
        ctx.replySuccess("Saltando a **" + FormatUtil.escape(handler.getQueue().get(0).getTrack().getInfo().title) + "**");
        handler.getPlayer().stopTrack();
    }

    @Override
    public void autocomplete(CommandAutoCompleteInteractionEvent event)
    {
        Autocomplete.queuePositions(bot, event, false);
    }
}
