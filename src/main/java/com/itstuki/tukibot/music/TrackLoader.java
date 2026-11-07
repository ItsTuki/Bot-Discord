/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
 * Copyright 2026 ItsTuki (lógica de PlayCmd/PlaynextCmd reescrita para interacciones).
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
import com.itstuki.tukibot.audio.RequestMetadata;
import com.itstuki.tukibot.commands.Checks;
import com.itstuki.tukibot.utils.FormatUtil;
import com.itstuki.tukibot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

/**
 * Carga canciones, playlists o búsquedas y las agrega a la cola. Es la
 * lógica de PlayCmd y PlaynextCmd de JMusicBot, ahora compartida por
 * /play, /playnext, el menú contextual y los menús de búsqueda.
 */
public class TrackLoader
{
    private static final Pattern URL = Pattern.compile("^(https?://|www\\.)\\S+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOCAL = Pattern.compile("^(/|\\./|\\.\\./|~|[a-zA-Z]:[\\\\/]|file:).*");
    private static final Pattern SEARCH_PREFIX = Pattern.compile("^(ytsearch|ytmsearch|scsearch|spsearch|amsearch|dzsearch):.*", Pattern.CASE_INSENSITIVE);

    private final Bot bot;

    public TrackLoader(Bot bot)
    {
        this.bot = bot;
    }

    public static boolean isUrl(String s)
    {
        return URL.matcher(s.trim()).matches();
    }

    /**
     * Normaliza lo que escribió el usuario. Las rutas locales solo las puede
     * usar el dueño del bot (antes cualquiera podía leer archivos del PC).
     */
    public String normalize(String query, Member member)
    {
        String q = query.trim();
        if(q.startsWith("<") && q.endsWith(">"))
            q = q.substring(1, q.length() - 1).trim();
        if(LOCAL.matcher(q).matches())
            return Checks.isOwner(bot, member.getUser()) ? q : "ytsearch:" + q;
        if(isUrl(q) || SEARCH_PREFIX.matcher(q).matches() || q.matches("^[a-zA-Z0-9_-]{11}$"))
            return q;
        // texto normal: se busca directo en YouTube
        return "ytsearch:" + q;
    }

    /**
     * @param hook  respuesta diferida que se va editando con el resultado
     * @param next  true para /playnext (se agrega al inicio de la cola)
     */
    public void load(Guild guild, Member member, InteractionHook hook, String query, boolean next)
    {
        String identifier = normalize(query, member);
        boolean search = SEARCH_PREFIX.matcher(identifier).matches();
        bot.getPlayerManager().loadItemOrdered(guild, identifier, new ResultHandler(guild, member, hook, identifier, next, search));
    }

    /** Agrega una pista ya cargada (por ejemplo, desde el menú de búsqueda). */
    public String addSingle(Guild guild, Member member, AudioTrack track, String query, boolean next)
    {
        if(bot.getConfig().isTooLong(track))
            return bot.getConfig().getWarning() + " **" + FormatUtil.escape(track.getInfo().title) + "** dura más de lo permitido: `"
                    + TimeUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`";
        AudioHandler handler = bot.getPlayerManager().setUpHandler(guild);
        QueuedTrack qt = new QueuedTrack(track, RequestMetadata.fromResultHandler(track, member.getUser(), query));
        int pos = (next ? handler.addTrackToFront(qt) : handler.addTrack(qt)) + 1;
        return bot.getConfig().getSuccess() + " Agregué " + FormatUtil.trackLink(track)
                + " (`" + TimeUtil.formatTime(track.getDuration()) + "`) "
                + (pos == 0 ? "y empieza a sonar ahora" : "a la cola en la posición " + pos);
    }

    /** Agrega todas las pistas de una playlist (menos {@code exclude}). */
    public int addPlaylist(Guild guild, Member member, AudioPlaylist playlist, AudioTrack exclude, String query)
    {
        AudioHandler handler = bot.getPlayerManager().setUpHandler(guild);
        int count = 0;
        for(AudioTrack track : playlist.getTracks())
        {
            if(bot.getConfig().isTooLong(track) || track.equals(exclude))
                continue;
            handler.addTrack(new QueuedTrack(track, RequestMetadata.fromResultHandler(track, member.getUser(), query)));
            count++;
        }
        return count;
    }

    /** Datos que espera el botón "cargar playlist". */
    public record PendingPlaylist(AudioPlaylist playlist, AudioTrack exclude, String query, String addMessage) {}

    private class ResultHandler implements AudioLoadResultHandler
    {
        private final Guild guild;
        private final Member member;
        private final InteractionHook hook;
        private final String query;
        private final boolean next;
        private final boolean search;

        private ResultHandler(Guild guild, Member member, InteractionHook hook, String query, boolean next, boolean search)
        {
            this.guild = guild;
            this.member = member;
            this.hook = hook;
            this.query = query;
            this.next = next;
            this.search = search;
        }

        private void edit(String text)
        {
            hook.editOriginal(FormatUtil.truncate(FormatUtil.filter(text), 2000)).queue(s -> {}, f -> {});
        }

        private void loadSingle(AudioTrack track, AudioPlaylist playlist)
        {
            String addMsg = addSingle(guild, member, track, query, next);
            if(playlist == null || next || bot.getConfig().isTooLong(track))
            {
                edit(addMsg);
                return;
            }
            String key = bot.getCache().put(new PendingPlaylist(playlist, track, query, addMsg), member.getIdLong(), 120);
            hook.editOriginal(new MessageEditBuilder()
                    .setContent(FormatUtil.truncate(addMsg + "\n" + bot.getConfig().getWarning() + " Esta canción viene con una playlist de **"
                            + playlist.getTracks().size() + "** canciones. ¿La cargo también?", 2000))
                    .setComponents(ActionRow.of(
                            Button.success("pl:" + key + ":yes", "Cargar playlist").withEmoji(Emoji.fromUnicode("📥")),
                            Button.secondary("pl:" + key + ":no", "Solo esta canción")))
                    .build()).queue(s -> {}, f -> {});
        }

        @Override
        public void trackLoaded(AudioTrack track)
        {
            loadSingle(track, null);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist)
        {
            if(playlist.getTracks().isEmpty())
            {
                edit(bot.getConfig().getWarning() + " La playlist " + (playlist.getName() == null ? "" : "(**" + FormatUtil.escape(playlist.getName()) + "**) ")
                        + "no se pudo cargar o está vacía.");
                return;
            }
            if(playlist.getTracks().size() == 1 || playlist.isSearchResult())
            {
                AudioTrack single = playlist.getSelectedTrack() == null ? playlist.getTracks().get(0) : playlist.getSelectedTrack();
                loadSingle(single, null);
            }
            else if(playlist.getSelectedTrack() != null)
            {
                loadSingle(playlist.getSelectedTrack(), playlist);
            }
            else if(next)
            {
                loadSingle(playlist.getTracks().get(0), null);
            }
            else
            {
                int count = addPlaylist(guild, member, playlist, null, query);
                String name = playlist.getName() == null ? "una playlist" : "la playlist **" + FormatUtil.escape(playlist.getName()) + "**";
                if(count == 0)
                    edit(bot.getConfig().getWarning() + " Todas las canciones de " + name + " duran más de lo permitido (`" + bot.getConfig().getMaxTime() + "`).");
                else
                    edit(bot.getConfig().getSuccess() + " Encontré " + name + " con `" + playlist.getTracks().size() + "` canciones; las agregué a la cola."
                            + (count < playlist.getTracks().size() ? "\n" + bot.getConfig().getWarning()
                                    + " Omití las que duran más de `" + bot.getConfig().getMaxTime() + "`." : ""));
            }
        }

        @Override
        public void noMatches()
        {
            if(search || isUrl(query))
                edit(bot.getConfig().getWarning() + " No encontré resultados para `" + FormatUtil.truncate(query.replace("ytsearch:", ""), 200) + "`.");
            else
                bot.getPlayerManager().loadItemOrdered(guild, "ytsearch:" + query, new ResultHandler(guild, member, hook, query, next, true));
        }

        @Override
        public void loadFailed(FriendlyException throwable)
        {
            if(throwable.severity == Severity.COMMON)
                edit(bot.getConfig().getError() + " Error al cargar: " + throwable.getMessage());
            else
                edit(bot.getConfig().getError() + " Error al cargar la canción. Si es de YouTube, revisa la consola: puede que haga falta activar OAuth.");
        }
    }
}
