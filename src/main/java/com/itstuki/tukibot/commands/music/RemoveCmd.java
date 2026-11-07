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
import com.itstuki.tukibot.audio.AudioHandler;
import com.itstuki.tukibot.audio.QueuedTrack;
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.music.Autocomplete;
import com.itstuki.tukibot.utils.FormatUtil;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class RemoveCmd extends SlashCommand
{
    public RemoveCmd(Bot bot)
    {
        super(bot);
        this.name = "remove";
        this.nameEs = "quitar";
        this.help = "Quita una canción de la cola (o todas las tuyas)";
        this.helpEn = "Removes a song from the queue";
        this.category = Category.MUSIC;
        this.beListening = true;
        this.bePlaying = true;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addOptions(Loc.option(OptionType.INTEGER, "position", "posicion",
                "Posición en la cola (0 = todas tus canciones)", "Queue position (0 = all your songs)", true)
                .setMinValue(0).setAutoComplete(true));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        AudioHandler handler = ctx.handler();
        if(handler.getQueue().isEmpty())
        {
            ctx.replyError("No hay nada en la cola.");
            return;
        }
        int pos = (int) ctx.integer("position", -1);
        if(pos == 0)
        {
            int count = handler.getQueue().removeAll(ctx.user().getIdLong());
            if(count == 0)
                ctx.replyWarning("No tienes canciones en la cola.");
            else
                ctx.replySuccess("Quité tus " + count + " canciones.");
            return;
        }
        if(pos < 1 || pos > handler.getQueue().size())
        {
            ctx.replyError("La posición tiene que estar entre 1 y " + handler.getQueue().size() + ".");
            return;
        }
        QueuedTrack qt = handler.getQueue().get(pos - 1);
        String title = FormatUtil.escape(qt.getTrack().getInfo().title);
        if(qt.getIdentifier() == ctx.user().getIdLong())
        {
            handler.getQueue().remove(pos - 1);
            ctx.replySuccess("Quité **" + title + "** de la cola.");
        }
        else if(Checks.isDJ(bot, ctx.member()))
        {
            handler.getQueue().remove(pos - 1);
            ctx.replySuccess("Quité **" + title + "** de la cola"
                    + (qt.getIdentifier() == 0 ? "." : " (la había pedido <@" + qt.getIdentifier() + ">)."));
        }
        else
            ctx.replyError("No puedes quitar **" + title + "** porque no la agregaste tú.");
    }

    @Override
    public void autocomplete(CommandAutoCompleteInteractionEvent event)
    {
        Autocomplete.queuePositions(bot, event, true);
    }
}
