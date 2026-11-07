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
import com.itstuki.tukibot.music.Autocomplete;
import com.itstuki.tukibot.music.TrackLoader;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class PlayCmd extends SlashCommand
{
    private final TrackLoader loader;
    protected boolean next = false;

    public PlayCmd(Bot bot)
    {
        super(bot);
        this.loader = new TrackLoader(bot);
        this.name = "play";
        this.nameEs = "reproducir";
        this.help = "Reproduce una canción o playlist: escribe el nombre o pega un enlace (YouTube, Spotify...)";
        this.helpEn = "Plays a song, playlist or stream (link or search)";
        this.category = Category.MUSIC;
        this.beListening = true;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        // obligatoria: así Discord pone el cursor en ella apenas escribes /play
        data.addOptions(
                Loc.option(OptionType.STRING, "query", "busqueda", "Nombre de la canción o enlace (YouTube, Spotify, SoundCloud, Twitch...)",
                        "Song name or link (YouTube, Spotify, SoundCloud...)", true).setAutoComplete(true));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        String query = ctx.string("query", "").trim();
        if(query.isEmpty())
        {
            ctx.replyError("Escribe el nombre de una canción o pega un enlace.");
            return;
        }
        ctx.defer(false);
        loader.load(ctx.guild(), ctx.member(), ctx.hook(), query, next);
    }

    @Override
    public void autocomplete(CommandAutoCompleteInteractionEvent event)
    {
        Autocomplete.youtube(bot, event);
    }
}
