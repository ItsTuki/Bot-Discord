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
package com.itstuki.tukibot.music;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.audio.AudioHandler;
import com.itstuki.tukibot.audio.QueuedTrack;
import com.itstuki.tukibot.utils.FormatUtil;
import com.itstuki.tukibot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;

/**
 * Sugerencias para las opciones de los comandos (Discord las pide mientras
 * se escribe y hay que responder en menos de 3 segundos).
 */
public final class Autocomplete
{
    private Autocomplete() {}

    private static void reply(CommandAutoCompleteInteractionEvent event, List<Choice> choices)
    {
        if(!event.isAcknowledged())
            event.replyChoices(choices.size() > 25 ? choices.subList(0, 25) : choices).queue(s -> {}, f -> {});
    }

    private static String choiceName(String text)
    {
        return FormatUtil.truncate(text, 100);
    }

    /** Sugerencias de YouTube para /play y /playnext. */
    public static void youtube(Bot bot, CommandAutoCompleteInteractionEvent event)
    {
        String input = event.getFocusedOption().getValue().trim();
        if(input.length() > 100 || TrackLoader.isUrl(input))
        {
            reply(event, input.length() <= 100 ? List.of(new Choice(choiceName("🔗 " + input), input)) : List.of());
            return;
        }
        if(!bot.getConfig().useAutocomplete() || input.length() < 3)
        {
            reply(event, input.isEmpty() ? List.of() : List.of(new Choice(choiceName("🔎 Buscar: " + input), input)));
            return;
        }
        bot.getPlayerManager().quickSearch("ytsearch:", input)
                .orTimeout(2300, TimeUnit.MILLISECONDS)
                .whenComplete((tracks, error) -> {
                    List<Choice> choices = new ArrayList<>();
                    choices.add(new Choice(choiceName("🔎 Buscar: " + input), input));
                    if(tracks != null)
                        for(AudioTrack t : tracks)
                        {
                            String uri = t.getInfo().uri;
                            if(uri == null || uri.length() > 100)
                                continue;
                            choices.add(new Choice(choiceName("🎵 " + t.getInfo().title + " — " + t.getInfo().author
                                    + " (" + TimeUtil.formatTime(t.getDuration()) + ")"), uri));
                            if(choices.size() >= 10)
                                break;
                        }
                    reply(event, choices);
                });
    }

    /** Posiciones de la cola, con el título de cada canción. */
    public static void queuePositions(Bot bot, CommandAutoCompleteInteractionEvent event, boolean includeMine)
    {
        if(event.getGuild() == null)
        {
            reply(event, List.of());
            return;
        }
        AudioHandler handler = bot.getPlayerManager().getHandler(event.getGuild());
        List<Choice> choices = new ArrayList<>();
        if(includeMine)
            choices.add(new Choice("🗑 Todas mis canciones", 0));
        if(handler != null)
        {
            String input = event.getFocusedOption().getValue().trim().toLowerCase(Locale.ROOT);
            List<QueuedTrack> list = handler.getQueue().getList();
            for(int i = 0; i < list.size() && choices.size() < 25; i++)
            {
                String title = list.get(i).getTrack().getInfo().title;
                String label = (i + 1) + ". " + title + " (" + TimeUtil.formatTime(list.get(i).getTrack().getDuration()) + ")";
                if(input.isEmpty() || label.toLowerCase(Locale.ROOT).contains(input))
                    choices.add(new Choice(choiceName(label), i + 1));
            }
        }
        reply(event, choices);
    }

    /** Nombres de las playlists guardadas. */
    public static void playlists(Bot bot, CommandAutoCompleteInteractionEvent event)
    {
        String input = event.getFocusedOption().getValue().trim().toLowerCase(Locale.ROOT);
        List<Choice> choices = new ArrayList<>();
        List<String> names = bot.getPlaylistLoader().getPlaylistNames();
        if(names != null)
            for(String n : names)
                if(input.isEmpty() || n.toLowerCase(Locale.ROOT).contains(input))
                    choices.add(new Choice(choiceName(n), n));
        reply(event, choices);
    }
}
