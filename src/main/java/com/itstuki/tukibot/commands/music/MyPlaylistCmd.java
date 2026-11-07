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
import com.itstuki.tukibot.audio.RequestMetadata;
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.music.Autocomplete;
import com.itstuki.tukibot.music.TrackLoader;
import com.itstuki.tukibot.userplaylist.UserPlaylists;
import com.itstuki.tukibot.userplaylist.UserPlaylists.Entry;
import com.itstuki.tukibot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * Playlists personales: cada usuario guarda las suyas y las puede poner en
 * cualquier servidor donde esté el bot.
 */
public class MyPlaylistCmd extends SlashCommand
{
    private final TrackLoader loader;

    public MyPlaylistCmd(Bot bot)
    {
        super(bot);
        this.name = "myplaylist";
        this.nameEs = "mi-playlist";
        this.help = "Tus playlists personales: guárdalas y ponlas en cualquier servidor";
        this.helpEn = "Your personal playlists, usable in any server";
        this.category = Category.MUSIC;
        this.musicChecks = false;
        this.loader = new TrackLoader(bot);
    }

    private UserPlaylists store()
    {
        return bot.getUserPlaylists();
    }

    private static OptionData nameOption()
    {
        return Loc.option(OptionType.STRING, "name", "nombre", "Nombre de tu playlist", "Playlist name", true)
                .setAutoComplete(true).setMaxLength(UserPlaylists.MAX_NAME);
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addSubcommands(
                Loc.sub("save", "guardar", "Guarda lo que está sonando y la cola como una playlist", "Saves the current song and queue")
                        .addOptions(nameOption(), Loc.option(OptionType.BOOLEAN, "queue", "cola",
                                "Incluir la cola (por defecto sí)", "Include the queue (default yes)", false)),
                Loc.sub("add", "agregar", "Agrega una canción (o la que está sonando) a tu playlist", "Adds a song to your playlist")
                        .addOptions(nameOption(), Loc.option(OptionType.STRING, "song", "cancion",
                                "Nombre o enlace (vacío = la que está sonando)", "Name or link (empty = current song)", false).setAutoComplete(true)),
                Loc.sub("play", "reproducir", "Pone una de tus playlists", "Plays one of your playlists")
                        .addOptions(nameOption(), Loc.option(OptionType.BOOLEAN, "shuffle", "mezclar",
                                "Mezclar el orden", "Shuffle the order", false)),
                Loc.sub("list", "lista", "Muestra tus playlists", "Lists your playlists"),
                Loc.sub("show", "ver", "Muestra las canciones de una playlist", "Shows a playlist's songs")
                        .addOptions(nameOption(), Loc.option(OptionType.INTEGER, "page", "pagina", "Página", "Page", false).setMinValue(1)),
                Loc.sub("remove", "quitar", "Quita una canción de tu playlist", "Removes a song from your playlist")
                        .addOptions(nameOption(), Loc.option(OptionType.INTEGER, "position", "posicion",
                                "Número de la canción (míralo con /myplaylist show)", "Song number", true).setMinValue(1)),
                Loc.sub("delete", "borrar", "Borra una de tus playlists", "Deletes one of your playlists").addOptions(nameOption()));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        String sub = ctx.event().getSubcommandName();
        long user = ctx.user().getIdLong();
        String pname = UserPlaylists.cleanName(ctx.string("name", ""));
        if(!"list".equals(sub) && pname.isEmpty())
        {
            ctx.replyError("Ponle un nombre a la playlist.");
            return;
        }
        switch(sub == null ? "" : sub)
        {
            case "save" -> save(ctx, user, pname);
            case "add" -> add(ctx, user, pname);
            case "play" -> play(ctx, user, pname);
            case "list" -> list(ctx, user);
            case "show" -> show(ctx, user, pname);
            case "remove" -> {
                Entry removed = store().removeAt(user, pname, (int) ctx.integer("position", 0));
                if(removed == null)
                    ctx.replyError("No encontré esa canción en **" + FormatUtil.escape(pname) + "**.");
                else
                    ctx.replySuccess("Quité **" + FormatUtil.escape(removed.title()) + "** de **" + FormatUtil.escape(store().realName(user, pname)) + "**.");
            }
            case "delete" -> {
                if(store().delete(user, pname))
                    ctx.replySuccess("Borré tu playlist **" + FormatUtil.escape(pname) + "**.");
                else
                    ctx.replyError("No tienes una playlist llamada **" + FormatUtil.escape(pname) + "**.");
            }
            default -> ctx.replyError("Subcomando desconocido.");
        }
    }

    private static Entry entry(AudioTrack track)
    {
        String uri = track.getInfo().uri;
        if(uri == null || uri.isBlank())
            uri = track.getIdentifier();
        return new Entry(track.getInfo().title, uri);
    }

    private void save(CommandContext ctx, long user, String pname)
    {
        AudioHandler handler = ctx.guild() == null ? null : bot.getPlayerManager().getHandler(ctx.guild());
        List<Entry> entries = new ArrayList<>();
        if(handler != null)
        {
            AudioTrack current = handler.getPlayer().getPlayingTrack();
            if(current != null)
                entries.add(entry(current));
            if(ctx.bool("queue", true))
                for(QueuedTrack qt : handler.getQueue().getList())
                    entries.add(entry(qt.getTrack()));
        }
        if(entries.isEmpty())
        {
            ctx.replyError("No hay nada sonando ni en cola para guardar. Usa `/myplaylist add` para agregar canciones una por una.");
            return;
        }
        boolean existed = store().realName(user, pname) != null;
        String error = store().save(user, pname, entries);
        if(error != null)
        {
            ctx.replyError(error);
            return;
        }
        ctx.replySuccess((existed ? "Reemplacé" : "Guardé") + " tu playlist **" + FormatUtil.escape(pname) + "** con **"
                + Math.min(entries.size(), UserPlaylists.MAX_TRACKS) + "** canciones."
                + "\n-# Ponla cuando quieras con " + bot.getCommands().mention("myplaylist play") + ".");
    }

    private void add(CommandContext ctx, long user, String pname)
    {
        String song = ctx.string("song", null);
        if(song == null || song.isBlank())
        {
            AudioHandler handler = ctx.guild() == null ? null : bot.getPlayerManager().getHandler(ctx.guild());
            AudioTrack current = handler == null ? null : handler.getPlayer().getPlayingTrack();
            if(current == null)
            {
                ctx.replyError("No hay nada sonando. Escribe el nombre o enlace de la canción en la opción **canción**.");
                return;
            }
            addEntries(ctx, user, pname, List.of(entry(current)));
            return;
        }
        ctx.defer(false);
        String identifier = loader.normalize(song, ctx.member());
        bot.getPlayerManager().loadItemOrdered(ctx.user(), identifier, new AudioLoadResultHandler()
        {
            @Override
            public void trackLoaded(AudioTrack track)
            {
                addEntries(ctx, user, pname, List.of(entry(track)));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist)
            {
                if(playlist.isSearchResult() || playlist.getSelectedTrack() != null)
                {
                    AudioTrack t = playlist.getSelectedTrack() != null ? playlist.getSelectedTrack() : playlist.getTracks().isEmpty() ? null : playlist.getTracks().get(0);
                    if(t == null)
                        noMatches();
                    else
                        addEntries(ctx, user, pname, List.of(entry(t)));
                    return;
                }
                List<Entry> entries = new ArrayList<>();
                playlist.getTracks().forEach(t -> entries.add(entry(t)));
                addEntries(ctx, user, pname, entries);
            }

            @Override
            public void noMatches()
            {
                ctx.replyWarning("No encontré nada para `" + FormatUtil.escape(song) + "`.");
            }

            @Override
            public void loadFailed(FriendlyException exception)
            {
                ctx.replyError("No pude cargar eso: " + exception.getMessage());
            }
        });
    }

    private void addEntries(CommandContext ctx, long user, String pname, List<Entry> entries)
    {
        boolean existed = store().realName(user, pname) != null;
        int added = store().append(user, pname, entries);
        String real = store().realName(user, pname);
        if(added < 0)
            ctx.replyError("Ya tienes " + UserPlaylists.MAX_PLAYLISTS + " playlists; borra alguna antes de crear otra.");
        else if(added == 0)
            ctx.replyWarning("**" + FormatUtil.escape(real) + "** ya tiene el máximo de " + UserPlaylists.MAX_TRACKS + " canciones.");
        else
            ctx.replySuccess((added == 1 ? "Agregué **" + FormatUtil.escape(entries.get(0).title()) + "**" : "Agregué **" + added + "** canciones")
                    + (existed ? " a" : " a tu nueva playlist") + " **" + FormatUtil.escape(real) + "** (" + store().size(user, real) + " en total).");
    }

    private void play(CommandContext ctx, long user, String pname)
    {
        List<Entry> entries = store().get(user, pname);
        if(entries == null || entries.isEmpty())
        {
            ctx.replyError(entries == null ? "No tienes una playlist llamada **" + FormatUtil.escape(pname) + "**." : "Esa playlist está vacía.");
            return;
        }
        String error = Checks.checkMusic(bot, ctx.member(), ctx.channel(), false, true);
        if(error != null)
        {
            ctx.replyError(error);
            return;
        }
        String real = store().realName(user, pname);
        List<Entry> order = new ArrayList<>(entries);
        if(ctx.bool("shuffle", false))
            Collections.shuffle(order);
        AudioHandler handler = ctx.handler();
        handler.setAnnounceChannel(ctx.channel());
        ctx.reply(bot.getConfig().getLoading() + " Cargando **" + FormatUtil.escape(real) + "** (" + order.size() + " canciones)...");

        AtomicInteger pending = new AtomicInteger(order.size());
        AtomicInteger loaded = new AtomicInteger();
        List<String> failed = Collections.synchronizedList(new ArrayList<>());
        // todas con la misma clave: lavaplayer las carga una tras otra y respeta el orden
        for(Entry e : order)
        {
            bot.getPlayerManager().loadItemOrdered(handler, e.uri(), new AudioLoadResultHandler()
            {
                private void finish()
                {
                    if(pending.decrementAndGet() > 0)
                        return;
                    StringBuilder sb = new StringBuilder(loaded.get() == 0
                            ? bot.getConfig().getWarning() + " No se pudo cargar ninguna canción de **" + FormatUtil.escape(real) + "**."
                            : bot.getConfig().getSuccess() + " Agregué **" + loaded.get() + "** canciones de tu playlist **" + FormatUtil.escape(real) + "**.");
                    if(!failed.isEmpty())
                    {
                        sb.append("\n-# No se pudieron cargar: ");
                        sb.append(String.join(", ", failed.subList(0, Math.min(5, failed.size()))));
                        if(failed.size() > 5)
                            sb.append(" y ").append(failed.size() - 5).append(" más");
                    }
                    ctx.hook().editOriginal(FormatUtil.truncate(FormatUtil.filter(sb.toString()), 2000)).queue(s -> {}, f -> {});
                }

                private void add(AudioTrack track)
                {
                    if(bot.getConfig().isTooLong(track))
                        failed.add(FormatUtil.escape(e.title()) + " (muy larga)");
                    else
                    {
                        handler.addTrack(new QueuedTrack(track, RequestMetadata.fromResultHandler(track, ctx.user(), e.uri())));
                        loaded.incrementAndGet();
                    }
                }

                @Override
                public void trackLoaded(AudioTrack track)
                {
                    add(track);
                    finish();
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist)
                {
                    AudioTrack t = playlist.getSelectedTrack() != null ? playlist.getSelectedTrack()
                            : playlist.getTracks().isEmpty() ? null : playlist.getTracks().get(0);
                    if(t == null)
                        failed.add(FormatUtil.escape(e.title()));
                    else
                        add(t);
                    finish();
                }

                @Override
                public void noMatches()
                {
                    failed.add(FormatUtil.escape(e.title()));
                    finish();
                }

                @Override
                public void loadFailed(FriendlyException exception)
                {
                    failed.add(FormatUtil.escape(e.title()));
                    finish();
                }
            });
        }
    }

    private void list(CommandContext ctx, long user)
    {
        List<String> names = store().names(user);
        if(names.isEmpty())
        {
            ctx.reply("📂 Todavía no tienes playlists. Pon música y usa " + bot.getCommands().mention("myplaylist save")
                    + ", o agrega canciones con " + bot.getCommands().mention("myplaylist add") + ".", true);
            return;
        }
        StringBuilder sb = new StringBuilder("### 📂 Tus playlists\n");
        for(String n : names)
            sb.append("- **").append(FormatUtil.escape(n)).append("** · ").append(store().size(user, n)).append(" canciones\n");
        sb.append("-# ").append(names.size()).append("/").append(UserPlaylists.MAX_PLAYLISTS).append(" playlists");
        ctx.reply(sb.toString(), true);
    }

    private void show(CommandContext ctx, long user, String pname)
    {
        List<Entry> entries = store().get(user, pname);
        if(entries == null)
        {
            ctx.replyError("No tienes una playlist llamada **" + FormatUtil.escape(pname) + "**.");
            return;
        }
        int pageSize = 15;
        int pages = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
        int page = (int) Math.min(pages, Math.max(1, ctx.integer("page", 1)));
        StringBuilder sb = new StringBuilder("### 🎶 " + FormatUtil.escape(store().realName(user, pname)) + "\n");
        if(entries.isEmpty())
            sb.append("*(vacía)*\n");
        for(int i = (page - 1) * pageSize; i < Math.min(entries.size(), page * pageSize); i++)
        {
            Entry e = entries.get(i);
            sb.append("`").append(i + 1).append(".` ");
            if(e.uri().startsWith("http"))
                sb.append("[").append(FormatUtil.escape(FormatUtil.truncate(e.title(), 70))).append("](<").append(e.uri()).append(">)");
            else
                sb.append(FormatUtil.escape(FormatUtil.truncate(e.title(), 70)));
            sb.append("\n");
        }
        sb.append("-# Página ").append(page).append("/").append(pages).append(" · ").append(entries.size()).append(" canciones");
        ctx.reply(FormatUtil.truncate(sb.toString(), 2000), true);
    }

    @Override
    public void autocomplete(CommandAutoCompleteInteractionEvent event)
    {
        if(event.getFocusedOption().getName().equals("song"))
        {
            Autocomplete.youtube(bot, event);
            return;
        }
        List<Choice> choices = new ArrayList<>();
        for(String n : store().search(event.getUser().getIdLong(), event.getFocusedOption().getValue()))
        {
            if(choices.size() >= 25)
                break;
            choices.add(new Choice(n, n));
        }
        // si escribe un nombre nuevo, se ofrece tal cual (sirve para guardar/agregar)
        String typed = UserPlaylists.cleanName(event.getFocusedOption().getValue());
        if(!typed.isEmpty() && choices.stream().noneMatch(c -> c.getName().equalsIgnoreCase(typed)) && choices.size() < 25)
            choices.add(new Choice(typed, typed));
        event.replyChoices(choices).queue(s -> {}, f -> {});
    }
}
