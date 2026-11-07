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
import com.itstuki.tukibot.music.TrackLoader;
import com.itstuki.tukibot.utils.FormatUtil;
import com.itstuki.tukibot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.List;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

/**
 * Busca en YouTube, YouTube Music o SoundCloud y muestra un menú de
 * selección con hasta 10 resultados (antes eran 4 con reacciones).
 * Reemplaza a SearchCmd y SCSearchCmd.
 */
public class SearchCmd extends SlashCommand implements ComponentHandler
{
    private final TrackLoader loader;

    public SearchCmd(Bot bot)
    {
        super(bot);
        this.loader = new TrackLoader(bot);
        this.name = "search";
        this.nameEs = "buscar";
        this.help = "Busca canciones y elige cuál reproducir";
        this.helpEn = "Searches for songs and lets you pick one";
        this.category = Category.MUSIC;
        this.beListening = true;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addOptions(
                Loc.option(OptionType.STRING, "query", "busqueda", "Qué quieres buscar", "What to search for", true),
                Loc.option(OptionType.STRING, "source", "fuente", "Dónde buscar (por defecto YouTube)", "Where to search", false)
                        .addChoice("YouTube", "ytsearch:")
                        .addChoice("YouTube Music", "ytmsearch:")
                        .addChoice("SoundCloud", "scsearch:"));
    }

    private record Results(List<AudioTrack> tracks, String query) {}

    @Override
    protected void execute(CommandContext ctx)
    {
        String query = ctx.string("query", "").trim();
        String prefix = ctx.string("source", "ytsearch:");
        ctx.defer(false);
        bot.getPlayerManager().loadItemOrdered(ctx.guild(), prefix + query, new AudioLoadResultHandler()
        {
            @Override
            public void trackLoaded(AudioTrack track)
            {
                ctx.hook().editOriginal(loader.addSingle(ctx.guild(), ctx.member(), track, query, false)).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist)
            {
                List<AudioTrack> tracks = playlist.getTracks().subList(0, Math.min(10, playlist.getTracks().size()));
                if(tracks.isEmpty())
                {
                    noMatches();
                    return;
                }
                String key = bot.getCache().put(new Results(List.copyOf(tracks), query), ctx.user().getIdLong(), 120);
                StringSelectMenu.Builder menu = StringSelectMenu.create("search:" + key)
                        .setPlaceholder("Elige una canción para agregarla a la cola");
                StringBuilder text = new StringBuilder(bot.getConfig().getSearching() + " Resultados para `")
                        .append(FormatUtil.truncate(query, 100).replace("`", "")).append("`:\n");
                for(int i = 0; i < tracks.size(); i++)
                {
                    AudioTrack t = tracks.get(i);
                    text.append("`").append(i + 1).append(".` `[").append(TimeUtil.formatTime(t.getDuration())).append("]` ")
                            .append(FormatUtil.trackLink(t)).append("\n");
                    menu.addOption(FormatUtil.truncate((i + 1) + ". " + t.getInfo().title, 100), Integer.toString(i),
                            FormatUtil.truncate(t.getInfo().author + " · " + TimeUtil.formatTime(t.getDuration()), 100),
                            Emoji.fromUnicode("🎵"));
                }
                text.append("-# El menú expira en 2 minutos.");
                ctx.hook().editOriginal(new MessageEditBuilder()
                        .setContent(FormatUtil.truncate(text.toString(), 2000))
                        .setComponents(ActionRow.of(menu.build()))
                        .build()).queue();
            }

            @Override
            public void noMatches()
            {
                ctx.hook().editOriginal(bot.getConfig().getWarning() + " No encontré resultados para `" + FormatUtil.truncate(query, 200) + "`.").queue();
            }

            @Override
            public void loadFailed(FriendlyException e)
            {
                ctx.hook().editOriginal(bot.getConfig().getError() + " Error al buscar"
                        + (e.severity == FriendlyException.Severity.COMMON ? ": " + e.getMessage() : ".")).queue();
            }
        });
    }

    @Override
    public void onSelect(StringSelectInteractionEvent event, String[] args)
    {
        var entry = bot.getCache().get(args.length > 0 ? args[0] : "");
        if(entry == null)
        {
            event.editMessage(bot.getConfig().getWarning() + " Esta búsqueda expiró. Usa " + bot.getCommands().mention(name) + " de nuevo.")
                    .setComponents().queue();
            return;
        }
        if(entry.ownerId() != event.getUser().getIdLong())
        {
            event.reply(bot.getConfig().getError() + " Solo quien hizo la búsqueda puede elegir.").setEphemeral(true).queue();
            return;
        }
        String error = Checks.checkMusic(bot, event.getMember(), event.getGuildChannel(), false, true);
        if(error != null)
        {
            event.reply(bot.getConfig().getError() + " " + error).setEphemeral(true).queue();
            return;
        }
        Results results = entry.as();
        int index = Integer.parseInt(event.getValues().get(0));
        AudioTrack track = results.tracks().get(index).makeClone();
        bot.getPlayerManager().setUpHandler(event.getGuild()).setAnnounceChannel(event.getGuildChannel());
        bot.getCache().remove(args[0]);
        event.editMessage(FormatUtil.filter(loader.addSingle(event.getGuild(), event.getMember(), track, results.query(), false)))
                .setComponents().queue();
    }
}
