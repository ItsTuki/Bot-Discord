/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 * Copyright 2026 ItsTuki (migración a JDA 6, DAVE y comandos slash).
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
package com.itstuki.tukibot;

import com.itstuki.tukibot.audio.AloneInVoiceHandler;
import com.itstuki.tukibot.audio.AudioHandler;
import com.itstuki.tukibot.audio.NowplayingHandler;
import com.itstuki.tukibot.audio.PlayerManager;
import com.itstuki.tukibot.commands.CommandManager;
import com.itstuki.tukibot.gui.GUI;
import com.itstuki.tukibot.lyrics.LyricsClient;
import com.itstuki.tukibot.playlist.PlaylistLoader;
import com.itstuki.tukibot.settings.SettingsManager;
import com.itstuki.tukibot.utils.InteractionCache;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;

/**
 * Contenedor de todos los componentes del bot.
 */
public class Bot
{
    public static final Activity DEFAULT_GAME = Activity.listening("/play · /help");

    private final ScheduledExecutorService threadpool;
    private final BotConfig config;
    private final SettingsManager settings;
    private final PlayerManager players;
    private final PlaylistLoader playlists;
    private final NowplayingHandler nowplaying;
    private final AloneInVoiceHandler aloneInVoiceHandler;
    private final InteractionCache cache;
    private final LyricsClient lyrics;
    private CommandManager commands;

    private volatile boolean shuttingDown = false;
    private JDA jda;
    private GUI gui;

    public Bot(BotConfig config, SettingsManager settings)
    {
        this.config = config;
        this.settings = settings;
        this.playlists = new PlaylistLoader(config);
        this.threadpool = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "TukiBot-Tareas");
            t.setDaemon(true);
            return t;
        });
        this.cache = new InteractionCache(threadpool);
        this.lyrics = new LyricsClient();
        this.players = new PlayerManager(this);
        this.players.init();
        this.nowplaying = new NowplayingHandler(this);
        this.nowplaying.init();
        this.aloneInVoiceHandler = new AloneInVoiceHandler(this);
        this.aloneInVoiceHandler.init();
    }

    public BotConfig getConfig() { return config; }
    public SettingsManager getSettingsManager() { return settings; }
    public ScheduledExecutorService getThreadpool() { return threadpool; }
    public PlayerManager getPlayerManager() { return players; }
    public PlaylistLoader getPlaylistLoader() { return playlists; }
    public NowplayingHandler getNowplayingHandler() { return nowplaying; }
    public AloneInVoiceHandler getAloneInVoiceHandler() { return aloneInVoiceHandler; }
    public InteractionCache getCache() { return cache; }
    public LyricsClient getLyrics() { return lyrics; }
    public CommandManager getCommands() { return commands; }
    public void setCommands(CommandManager commands) { this.commands = commands; }
    public JDA getJDA() { return jda; }

    public void closeAudioConnection(long guildId)
    {
        Guild guild = jda.getGuildById(guildId);
        if(guild!=null)
            threadpool.submit(() -> {
                nowplaying.setVoiceStatus(guild, "");
                guild.getAudioManager().closeAudioConnection();
            });
    }

    public Activity getDefaultGame()
    {
        if(config.isGameNone())
            return null;
        return config.getGame() == null ? DEFAULT_GAME : config.getGame();
    }

    public void resetGame()
    {
        Activity game = getDefaultGame();
        if(!Objects.equals(jda.getPresence().getActivity(), game))
            jda.getPresence().setActivity(game);
    }

    public void shutdown()
    {
        if(shuttingDown)
            return;
        shuttingDown = true;
        threadpool.shutdownNow();
        if(jda != null && jda.getStatus()!=JDA.Status.SHUTTING_DOWN && jda.getStatus()!=JDA.Status.SHUTDOWN)
        {
            jda.getGuilds().forEach(g ->
            {
                AudioHandler ah = (AudioHandler)g.getAudioManager().getSendingHandler();
                if(ah!=null)
                {
                    ah.stopAndClear();
                    ah.getPlayer().destroy();
                }
                g.getAudioManager().closeAudioConnection();
            });
            jda.shutdown();
        }
        if(gui!=null)
            gui.dispose();
        System.exit(0);
    }

    public void setJDA(JDA jda) { this.jda = jda; }
    public void setGUI(GUI gui) { this.gui = gui; }
}
