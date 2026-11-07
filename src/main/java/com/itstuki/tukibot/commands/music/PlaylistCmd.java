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
import com.itstuki.tukibot.playlist.PlaylistLoader;
import com.itstuki.tukibot.playlist.PlaylistLoader.Playlist;
import com.itstuki.tukibot.utils.FormatUtil;
import java.io.IOException;
import java.util.List;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * Playlists guardadas en la carpeta Playlists. Junta "play playlist",
 * "playlists" y el "playlist" de dueño de JMusicBot en un solo comando con
 * subcomandos. Crear, agregar y borrar sigue siendo solo para el dueño.
 */
public class PlaylistCmd extends SlashCommand
{
    public PlaylistCmd(Bot bot)
    {
        super(bot);
        this.name = "playlist";
        this.nameEs = "playlist";
        this.help = "Reproduce o administra las playlists guardadas";
        this.helpEn = "Plays or manages saved playlists";
        this.category = Category.MUSIC;
        this.musicChecks = false;
    }

    private static OptionData nameOption(boolean autocomplete)
    {
        return Loc.option(OptionType.STRING, "name", "nombre", "Nombre de la playlist", "Playlist name", true).setAutoComplete(autocomplete);
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addSubcommands(
                Loc.sub("play", "reproducir", "Agrega una playlist guardada a la cola", "Plays a saved playlist").addOptions(nameOption(true)),
                Loc.sub("list", "lista", "Muestra las playlists disponibles", "Lists the available playlists"),
                Loc.sub("create", "crear", "(Dueño) Crea una playlist vacía", "(Owner) Creates an empty playlist").addOptions(nameOption(false)),
                Loc.sub("append", "agregar", "(Dueño) Agrega enlaces o búsquedas a una playlist", "(Owner) Appends songs to a playlist")
                        .addOptions(nameOption(true), Loc.option(OptionType.STRING, "songs", "canciones",
                                "Enlaces o búsquedas separados por |", "Links or searches separated by |", true)),
                Loc.sub("delete", "borrar", "(Dueño) Borra una playlist", "(Owner) Deletes a playlist").addOptions(nameOption(true)));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        String sub = ctx.event().getSubcommandName();
        String pname = PlaylistLoader.sanitize(ctx.string("name", ""));
        PlaylistLoader loader = bot.getPlaylistLoader();
        if(!"play".equals(sub) && !"list".equals(sub) && !Checks.isOwner(bot, ctx.user()))
        {
            ctx.replyError("Solo el dueño del bot puede modificar las playlists.");
            return;
        }
        switch(sub == null ? "" : sub)
        {
            case "play" -> play(ctx, pname);
            case "list" -> {
                if(!loader.folderExists())
                    loader.createFolder();
                List<String> list = loader.getPlaylistNames();
                if(list == null)
                    ctx.replyError("No pude leer la carpeta de playlists.");
                else if(list.isEmpty())
                    ctx.replyWarning("No hay playlists en la carpeta **" + bot.getConfig().getPlaylistsFolder() + "**.");
                else
                {
                    StringBuilder sb = new StringBuilder(bot.getConfig().getSuccess() + " Playlists disponibles:\n");
                    list.forEach(s -> sb.append("`").append(s).append("` "));
                    sb.append("\nUsa ").append(bot.getCommands().mention("playlist play")).append(" para reproducir una.");
                    ctx.reply(sb.toString());
                }
            }
            case "create" -> {
                if(pname.isEmpty())
                    ctx.replyError("Ese nombre no es válido (usa letras, números, _ o -).");
                else if(loader.getPlaylist(pname) != null)
                    ctx.replyError("La playlist `" + pname + "` ya existe.");
                else
                    try
                    {
                        loader.createPlaylist(pname);
                        ctx.replySuccess("Creé la playlist `" + pname + "`.");
                    }
                    catch(IOException e)
                    {
                        ctx.replyError("No pude crear la playlist: " + e.getLocalizedMessage());
                    }
            }
            case "append" -> {
                Playlist playlist = loader.getPlaylist(pname);
                if(playlist == null)
                {
                    ctx.replyError("La playlist `" + pname + "` no existe.");
                    return;
                }
                StringBuilder builder = new StringBuilder();
                playlist.getItems().forEach(item -> builder.append("\n").append(item));
                String[] urls = ctx.string("songs", "").split("\\|");
                int added = 0;
                for(String url : urls)
                {
                    String u = url.trim();
                    if(u.startsWith("<") && u.endsWith(">"))
                        u = u.substring(1, u.length() - 1);
                    if(u.isEmpty())
                        continue;
                    builder.append("\n").append(u);
                    added++;
                }
                try
                {
                    loader.writePlaylist(pname, builder.toString());
                    ctx.replySuccess("Agregué " + added + " elementos a la playlist `" + pname + "`.");
                }
                catch(IOException e)
                {
                    ctx.replyError("No pude escribir la playlist: " + e.getLocalizedMessage());
                }
            }
            case "delete" -> {
                if(loader.getPlaylist(pname) == null)
                    ctx.replyError("La playlist `" + pname + "` no existe.");
                else
                    try
                    {
                        loader.deletePlaylist(pname);
                        ctx.replySuccess("Borré la playlist `" + pname + "`.");
                    }
                    catch(IOException e)
                    {
                        ctx.replyError("No pude borrar la playlist: " + e.getLocalizedMessage());
                    }
            }
            default -> ctx.replyError("Subcomando desconocido.");
        }
    }

    private void play(CommandContext ctx, String pname)
    {
        String error = Checks.checkMusic(bot, ctx.member(), ctx.channel(), false, true);
        if(error != null)
        {
            ctx.replyError(error);
            return;
        }
        Playlist playlist = bot.getPlaylistLoader().getPlaylist(pname);
        if(playlist == null)
        {
            ctx.replyError("No encontré `" + pname + ".txt` en la carpeta de playlists.");
            return;
        }
        AudioHandler handler = ctx.handler();
        handler.setAnnounceChannel(ctx.channel());
        ctx.reply(bot.getConfig().getLoading() + " Cargando la playlist **" + pname + "**... (" + playlist.getItems().size() + " elementos)");
        playlist.loadTracks(bot.getPlayerManager(),
                at -> handler.addTrack(new QueuedTrack(at, RequestMetadata.fromResultHandler(at, ctx.user(), pname))),
                () -> {
                    StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                            ? bot.getConfig().getWarning() + " No se cargó ninguna canción."
                            : bot.getConfig().getSuccess() + " Cargué **" + playlist.getTracks().size() + "** canciones de **" + pname + "**.");
                    if(!playlist.getErrors().isEmpty())
                        builder.append("\nEstas no se pudieron cargar:");
                    playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1).append("]` **")
                            .append(FormatUtil.escape(err.getItem())).append("**: ").append(err.getReason()));
                    ctx.hook().editOriginal(FormatUtil.truncate(FormatUtil.filter(builder.toString()), 2000)).queue();
                });
    }

    @Override
    public void autocomplete(CommandAutoCompleteInteractionEvent event)
    {
        Autocomplete.playlists(bot, event);
    }
}
