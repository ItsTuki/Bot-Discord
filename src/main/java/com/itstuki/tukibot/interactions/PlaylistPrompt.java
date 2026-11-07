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
package com.itstuki.tukibot.interactions;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.commands.ComponentHandler;
import com.itstuki.tukibot.music.TrackLoader;
import com.itstuki.tukibot.music.TrackLoader.PendingPlaylist;
import com.itstuki.tukibot.utils.FormatUtil;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

/**
 * Botones "Cargar playlist" / "Solo esta canción" que aparecen al pegar un
 * enlace de YouTube que trae una lista (antes era la reacción 📥).
 */
public class PlaylistPrompt implements ComponentHandler
{
    private final Bot bot;
    private final TrackLoader loader;

    public PlaylistPrompt(Bot bot)
    {
        this.bot = bot;
        this.loader = new TrackLoader(bot);
    }

    @Override
    public void onButton(ButtonInteractionEvent event, String[] args)
    {
        var entry = args.length < 2 ? null : bot.getCache().get(args[0]);
        if(entry == null)
        {
            event.editComponents().queue();
            return;
        }
        if(entry.ownerId() != event.getUser().getIdLong())
        {
            event.reply(bot.getConfig().getError() + " Solo quien pidió la canción puede elegir.").setEphemeral(true).queue();
            return;
        }
        PendingPlaylist pending = entry.as();
        bot.getCache().remove(args[0]);
        if(args[1].equals("yes"))
        {
            int count = loader.addPlaylist(event.getGuild(), event.getMember(), pending.playlist(), pending.exclude(), pending.query());
            event.editMessage(FormatUtil.truncate(pending.addMessage() + "\n" + bot.getConfig().getSuccess()
                    + " Cargué **" + count + "** canciones más de la playlist.", 2000)).setComponents().queue();
        }
        else
            event.editMessage(pending.addMessage()).setComponents().queue();
    }
}
