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
import com.itstuki.tukibot.audio.QueuedTrack;
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.music.Autocomplete;
import com.itstuki.tukibot.queue.AbstractQueue;
import com.itstuki.tukibot.utils.FormatUtil;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class MoveTrackCmd extends SlashCommand
{
    public MoveTrackCmd(Bot bot)
    {
        super(bot);
        this.name = "movetrack";
        this.nameEs = "mover";
        this.help = "Mueve una canción a otra posición de la cola";
        this.helpEn = "Moves a track in the queue to a different position";
        this.category = Category.DJ;
        this.bePlaying = true;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addOptions(
                Loc.option(OptionType.INTEGER, "from", "desde", "Posición actual", "Current position", true).setMinValue(1).setAutoComplete(true),
                Loc.option(OptionType.INTEGER, "to", "hasta", "Nueva posición", "New position", true).setMinValue(1).setAutoComplete(true));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        int from = (int) ctx.integer("from", 0);
        int to = (int) ctx.integer("to", 0);
        if(from == to)
        {
            ctx.replyError("No puedo mover una canción a la misma posición.");
            return;
        }
        AbstractQueue<QueuedTrack> queue = ctx.handler().getQueue();
        for(int p : new int[]{from, to})
            if(p < 1 || p > queue.size())
            {
                ctx.replyError("`" + p + "` no es una posición válida de la cola.");
                return;
            }
        QueuedTrack track = queue.moveItem(from - 1, to - 1);
        ctx.replySuccess("Moví **" + FormatUtil.escape(track.getTrack().getInfo().title) + "** de la posición `" + from + "` a la `" + to + "`.");
    }

    @Override
    public void autocomplete(CommandAutoCompleteInteractionEvent event)
    {
        Autocomplete.queuePositions(bot, event, false);
    }
}
