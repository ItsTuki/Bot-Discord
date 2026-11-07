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
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.music.Autocomplete;
import com.itstuki.tukibot.music.TrackLoader;
import com.itstuki.tukibot.utils.FormatUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
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
        this.help = "Reproduce una canción, playlist o transmisión (enlace o búsqueda)";
        this.helpEn = "Plays a song, playlist or stream (link or search)";
        this.category = Category.MUSIC;
        this.beListening = true;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addOptions(
                Loc.option(OptionType.STRING, "query", "busqueda", "Nombre de la canción o enlace (YouTube, SoundCloud, Bandcamp, Twitch...)",
                        "Song name or link", false).setAutoComplete(true),
                Loc.option(OptionType.ATTACHMENT, "file", "archivo", "Archivo de audio para reproducir", "Audio file to play", false));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        String query = ctx.string("query", "").trim();
        OptionMapping file = ctx.option("file");
        if(query.isEmpty() && file == null)
        {
            AudioHandler handler = ctx.handler();
            if(handler.getPlayer().getPlayingTrack() != null && handler.getPlayer().isPaused())
            {
                if(Checks.isDJ(bot, ctx.member()))
                {
                    handler.getPlayer().setPaused(false);
                    bot.getNowplayingHandler().refresh(ctx.guild());
                    ctx.replySuccess("Reanudé **" + FormatUtil.escape(handler.getPlayer().getPlayingTrack().getInfo().title) + "**.");
                }
                else
                    ctx.replyError("Solo los DJ pueden reanudar la música.");
                return;
            }
            ctx.reply(bot.getConfig().getWarning() + " Cómo usar " + bot.getCommands().mention(name) + ":\n"
                    + "• Escribe el nombre de una canción y elige una sugerencia\n"
                    + "• Pega un enlace de YouTube, SoundCloud, Bandcamp, Vimeo o Twitch\n"
                    + "• Adjunta un archivo de audio en la opción **archivo**\n"
                    + "• Para tus playlists guardadas usa " + bot.getCommands().mention("playlist play"), true);
            return;
        }
        if(query.isEmpty())
        {
            Message.Attachment att = file.getAsAttachment();
            query = att.getUrl();
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
