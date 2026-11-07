/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
 * Copyright 2026 ItsTuki (migración a JDA 6, DAVE y paneles con botones).
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
package com.itstuki.tukibot.audio;

import com.itstuki.tukibot.playlist.PlaylistLoader.Playlist;
import com.itstuki.tukibot.queue.AbstractQueue;
import com.itstuki.tukibot.settings.QueueType;
import com.itstuki.tukibot.settings.RepeatMode;
import com.itstuki.tukibot.settings.Settings;
import com.itstuki.tukibot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reproductor de un servidor: maneja la cola, la repetición y entrega el
 * audio a JDA.
 */
public class AudioHandler extends AudioEventAdapter implements AudioSendHandler
{
    private static final Logger LOG = LoggerFactory.getLogger("AudioHandler");

    public final static String PLAY_EMOJI  = "▶"; // ▶
    public final static String PAUSE_EMOJI = "⏸"; // ⏸
    public final static String STOP_EMOJI  = "⏹"; // ⏹

    private final List<AudioTrack> defaultQueue = new LinkedList<>();
    private final Set<String> votes = ConcurrentHashMap.newKeySet();

    private final PlayerManager manager;
    private final AudioPlayer audioPlayer;
    private final long guildId;

    // Se reutiliza el mismo búfer para no crear basura cada 20 ms.
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final MutableAudioFrame frame = new MutableAudioFrame();

    private AbstractQueue<QueuedTrack> queue;
    private volatile long announceChannelId = 0L;

    // Autoplay: no se activa al usar /stop, y evita repetir lo último que sonó.
    private static final int HISTORY_SIZE = 50;
    private static final int AUTOPLAY_BATCH = 5;
    private final Deque<String> history = new ArrayDeque<>();
    private volatile boolean stopping = false;
    private volatile AudioTrack lastTrack = null;

    private volatile AudioFilter filter = AudioFilter.OFF;

    protected AudioHandler(PlayerManager manager, Guild guild, AudioPlayer player)
    {
        this.manager = manager;
        this.audioPlayer = player;
        this.guildId = guild.getIdLong();
        this.frame.setBuffer(buffer);
        this.setQueueType(manager.getBot().getSettingsManager().getSettings(guildId).getQueueType());
    }

    public void setQueueType(QueueType type)
    {
        queue = type.createInstance(queue);
    }

    private QueuedTrack prepare(QueuedTrack qtrack)
    {
        AudioTrack prepared = manager.prepare(qtrack.getTrack());
        return prepared == qtrack.getTrack() ? qtrack : new QueuedTrack(prepared, qtrack.getRequestMetadata());
    }

    public int addTrackToFront(QueuedTrack qtrack)
    {
        qtrack = prepare(qtrack);
        if(audioPlayer.getPlayingTrack()==null)
        {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        }
        else
        {
            queue.addAt(0, qtrack);
            manager.prefetch(qtrack.getTrack());
            return 0;
        }
    }

    public int addTrack(QueuedTrack qtrack)
    {
        qtrack = prepare(qtrack);
        if(audioPlayer.getPlayingTrack()==null)
        {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        }
        else
        {
            int index = queue.add(qtrack);
            if(index == 0)
                manager.prefetch(qtrack.getTrack());
            return index;
        }
    }

    public AbstractQueue<QueuedTrack> getQueue()
    {
        return queue;
    }

    public void stopAndClear()
    {
        stopping = true;
        try
        {
            queue.clear();
            defaultQueue.clear();
            audioPlayer.stopTrack();
            audioPlayer.setPaused(false);
            setFilter(AudioFilter.OFF);
        }
        finally
        {
            stopping = false;
        }
    }

    /** Aplica un filtro de audio (o lo quita con {@link AudioFilter#OFF}). */
    public void setFilter(AudioFilter filter)
    {
        this.filter = filter == null ? AudioFilter.OFF : filter;
        audioPlayer.setFilterFactory(this.filter.factory());
    }

    public AudioFilter getFilter()
    {
        return filter;
    }

    public boolean isAutoplay()
    {
        Settings s = manager.getBot().getSettingsManager().getSettings(guildId);
        return s != null && s.isAutoplay();
    }

    public boolean isMusicPlaying(JDA jda)
    {
        Guild guild = guild(jda);
        return guild != null && guild.getSelfMember().getVoiceState() != null
                && guild.getSelfMember().getVoiceState().inAudioChannel()
                && audioPlayer.getPlayingTrack()!=null;
    }

    public Set<String> getVotes()
    {
        return votes;
    }

    public AudioPlayer getPlayer()
    {
        return audioPlayer;
    }

    public long getGuildId()
    {
        return guildId;
    }

    /** Canal donde se publican los avisos y el panel automático. */
    public void setAnnounceChannel(GuildMessageChannel channel)
    {
        if(channel != null)
            this.announceChannelId = channel.getIdLong();
    }

    public long getAnnounceChannelId()
    {
        return announceChannelId;
    }

    public RequestMetadata getRequestMetadata()
    {
        if(audioPlayer.getPlayingTrack() == null)
            return RequestMetadata.EMPTY;
        RequestMetadata rm = audioPlayer.getPlayingTrack().getUserData(RequestMetadata.class);
        return rm == null ? RequestMetadata.EMPTY : rm;
    }

    public boolean playFromDefault()
    {
        if(!defaultQueue.isEmpty())
        {
            audioPlayer.playTrack(defaultQueue.remove(0));
            return true;
        }
        Settings settings = manager.getBot().getSettingsManager().getSettings(guildId);
        if(settings==null || settings.getDefaultPlaylist()==null)
            return false;

        Playlist pl = manager.getBot().getPlaylistLoader().getPlaylist(settings.getDefaultPlaylist());
        if(pl==null || pl.getItems().isEmpty())
            return false;
        pl.loadTracks(manager, (loaded) ->
        {
            AudioTrack at = manager.prepare(loaded);
            if(audioPlayer.getPlayingTrack()==null)
                audioPlayer.playTrack(at);
            else
                defaultQueue.add(at);
        }, () ->
        {
            if(pl.getTracks().isEmpty() && !manager.getBot().getConfig().getStay())
                manager.getBot().closeAudioConnection(guildId);
        });
        return true;
    }

    // Eventos de audio
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason)
    {
        remember(track);
        RepeatMode repeatMode = manager.getBot().getSettingsManager().getSettings(guildId).getRepeatMode();
        // si terminó normalmente y está la repetición activada, se vuelve a agregar
        if(endReason==AudioTrackEndReason.FINISHED && repeatMode != RepeatMode.OFF)
        {
            QueuedTrack clone = new QueuedTrack(track.makeClone(), track.getUserData(RequestMetadata.class));
            if(repeatMode == RepeatMode.ALL)
                queue.add(clone);
            else
                queue.addAt(0, clone);
        }

        if(!endReason.mayStartNext && endReason != AudioTrackEndReason.STOPPED)
            return; // REPLACED o CLEANUP: otra pista ya tomó su lugar

        if(queue.isEmpty())
        {
            if(!playFromDefault())
            {
                if(!stopping && endReason != AudioTrackEndReason.LOAD_FAILED && isAutoplay() && startAutoplay(track))
                    return;
                finishEmpty();
            }
        }
        else
        {
            QueuedTrack qt = queue.pull();
            player.playTrack(qt.getTrack());
        }
    }

    /** Se terminó la cola y no hay nada más que poner. */
    private void finishEmpty()
    {
        manager.getBot().getNowplayingHandler().onTrackUpdate(guildId, null);
        if(!manager.getBot().getConfig().getStay())
            manager.getBot().closeAudioConnection(guildId);
        // se quita la pausa para que la próxima canción no empiece pausada
        audioPlayer.setPaused(false);
    }

    // ---------------------------------------------------------------- autoplay

    private void remember(AudioTrack track)
    {
        String id = manager.youtubeId(track);
        if(id == null)
            return;
        synchronized(history)
        {
            history.remove(id);
            history.addLast(id);
            while(history.size() > HISTORY_SIZE)
                history.removeFirst();
        }
    }

    private boolean recentlyPlayed(String id)
    {
        synchronized(history)
        {
            return history.contains(id);
        }
    }

    /**
     * Busca canciones parecidas a la que terminó (el "mix" de YouTube) y las
     * agrega a la cola. Devuelve false si no hay de dónde sacar una semilla.
     */
    private boolean startAutoplay(AudioTrack ended)
    {
        AudioTrack seed = ended != null ? ended : lastTrack;
        if(seed == null || manager.getBot().getJDA() == null)
            return false;
        String id = manager.youtubeId(seed);
        String query = id != null
                ? "https://www.youtube.com/watch?v=" + id + "&list=RD" + id
                : "ytsearch:" + seed.getInfo().author + " " + seed.getInfo().title;
        LOG.info("Autoplay en " + guildId + ": buscando canciones parecidas a " + seed.getInfo().title);
        manager.loadItemOrdered(this, query, new AudioLoadResultHandler()
        {
            @Override
            public void trackLoaded(AudioTrack track)
            {
                done(List.of(track));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist)
            {
                done(playlist.getTracks());
            }

            @Override
            public void noMatches()
            {
                done(List.of());
            }

            @Override
            public void loadFailed(FriendlyException exception)
            {
                LOG.warn("Autoplay: no se pudo cargar el mix: " + exception.getMessage());
                done(List.of());
            }

            private void done(List<AudioTrack> tracks)
            {
                // Mientras tanto alguien pudo haber puesto otra canción o usado /stop.
                if(audioPlayer.getPlayingTrack() != null || !queue.isEmpty())
                    return;
                String seedId = id;
                List<AudioTrack> picked = new ArrayList<>();
                for(AudioTrack t : tracks)
                {
                    if(picked.size() >= AUTOPLAY_BATCH)
                        break;
                    String tid = t.getIdentifier();
                    if(t.getInfo().isStream || tid.equals(seedId) || recentlyPlayed(tid)
                            || manager.getBot().getConfig().isTooLong(t))
                        continue;
                    picked.add(t);
                }
                if(picked.isEmpty())
                {
                    manager.getBot().getNowplayingHandler().announce(guildId,
                            "📻 No encontré más canciones parecidas para el autoplay, así que me detengo.");
                    finishEmpty();
                    return;
                }
                for(AudioTrack t : picked)
                    addTrack(new QueuedTrack(t, RequestMetadata.EMPTY));
            }
        });
        return true;
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception)
    {
        LOG.error("No se pudo reproducir la pista " + track.getIdentifier(), exception);
        String hint = "";
        boolean loginRequired = false;
        for(Throwable t = exception; t != null; t = t.getCause())
            if(t.getMessage() != null && (t.getMessage().contains("requires login") || t.getMessage().contains("Sign in")
                    || t.getMessage().contains("not a bot")))
                loginRequired = true;
        String source = track.getSourceManager() == null ? null : track.getSourceManager().getSourceName();
        if(loginRequired && ("youtube".equals(source) || "spotify".equals(source)))
        {
            var config = manager.getBot().getConfig();
            boolean hasCookies = config.getYoutubeCookies() != null || config.getYoutubeCookiesBrowser() != null;
            hint = hasCookies
                    ? "\n-# YouTube está pidiendo iniciar sesión aunque hay cookies configuradas; puede que hayan vencido. Ya le avisé al dueño del bot."
                    : "\n-# YouTube está pidiendo iniciar sesión. Ya le avisé al dueño del bot.";
            manager.getBot().getAlerts().send("youtube-login", hasCookies
                    ? "\u26A0\uFE0F **TukiBot: YouTube está bloqueando la música.**\n"
                        + "Responde \"Sign in to confirm you're not a bot\" aunque hay cookies configuradas, así que probablemente **vencieron**.\n"
                        + "Exporta un `cookies.txt` nuevo (ventana de incógnito → cuenta secundaria → extensión *Get cookies.txt LOCALLY* en youtube.com/robots.txt → cerrar incógnito), reemplázalo junto al bot y reinicia."
                    : "\u26A0\uFE0F **TukiBot: YouTube está bloqueando la música.**\n"
                        + "Responde \"Sign in to confirm you're not a bot\" (pasa sobre todo en VPS). Para arreglarlo, deja un `cookies.txt` de una cuenta secundaria junto a `TukiBot.jar` y reinicia. "
                        + "Pasos: https://github.com/ItsTuki/Bot-Discord#youtube",
                    6L * 60 * 60 * 1000);
        }
        manager.getBot().getNowplayingHandler().announce(guildId, manager.getBot().getConfig().getError()
                + " No se pudo reproducir **" + FormatUtil.escape(track.getInfo().title) + "**"
                + (exception.severity == FriendlyException.Severity.COMMON ? ": " + exception.getMessage() : ".") + hint);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs)
    {
        LOG.warn("La pista " + track.getIdentifier() + " se trabó; se salta.");
        player.stopTrack();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track)
    {
        votes.clear();
        lastTrack = track;
        remember(track);
        manager.getBot().getNowplayingHandler().onTrackUpdate(guildId, track);
        QueuedTrack next = queue.isEmpty() ? null : queue.get(0);
        if(next != null)
            manager.prefetch(next.getTrack());
    }

    public String getStatusEmoji()
    {
        return audioPlayer.isPaused() ? PAUSE_EMOJI : PLAY_EMOJI;
    }

    // Métodos de AudioSendHandler
    @Override
    public boolean canProvide()
    {
        return audioPlayer.provide(frame);
    }

    @Override
    public ByteBuffer provide20MsAudio()
    {
        return buffer.flip();
    }

    @Override
    public boolean isOpus()
    {
        return true;
    }

    private Guild guild(JDA jda)
    {
        return jda.getGuildById(guildId);
    }
}
