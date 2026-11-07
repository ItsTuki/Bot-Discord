/*
 * Copyright 2016 John Grosh (jagrosh).
 * Copyright 2026 ItsTuki (JDA 6, DAVE, comandos slash e integración con el bot original).
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

import ch.qos.logback.classic.Level;
import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import com.itstuki.tukibot.commands.CommandManager;
import com.itstuki.tukibot.commands.admin.ConfigCmd;
import com.itstuki.tukibot.commands.dj.*;
import com.itstuki.tukibot.commands.general.*;
import com.itstuki.tukibot.commands.music.*;
import com.itstuki.tukibot.commands.owner.OwnerCmd;
import com.itstuki.tukibot.entities.Prompt;
import com.itstuki.tukibot.gui.GUI;
import com.itstuki.tukibot.interactions.*;
import com.itstuki.tukibot.settings.SettingsManager;
import com.itstuki.tukibot.utils.OtherUtil;
import java.util.EnumSet;
import java.util.concurrent.Executors;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada de TukiBot.
 */
public class TukiBot
{
    public final static Logger LOG = LoggerFactory.getLogger(TukiBot.class);

    public final static Permission[] RECOMMENDED_PERMS = {
        Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_SEND_IN_THREADS, Permission.MESSAGE_HISTORY,
        Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MANAGE, Permission.MESSAGE_EXT_EMOJI,
        Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VOICE_SET_STATUS, Permission.NICKNAME_CHANGE
    };

    public static void main(String[] args)
    {
        if(args.length > 0 && args[0].equalsIgnoreCase("generate-config"))
        {
            BotConfig.writeDefaultConfig();
            return;
        }
        startBot();
    }

    private static void startBot()
    {
        Prompt prompt = new Prompt("TukiBot");
        OtherUtil.checkJavaVersion(prompt);

        BotConfig config = new BotConfig(prompt);
        config.load();
        if(!config.isValid())
            return;
        LOG.info("Configuración cargada desde " + config.getConfigLocation());

        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(
                Level.toLevel(config.getLogLevel(), Level.INFO));

        // nadie recibe menciones por accidente desde los mensajes del bot
        MessageRequest.setDefaultMentions(EnumSet.noneOf(Message.MentionType.class));

        SettingsManager settings = new SettingsManager();
        Bot bot = new Bot(config, settings);
        CommandManager commands = createCommandManager(bot);
        bot.setCommands(commands);

        if(!prompt.isNoGUI())
        {
            try
            {
                GUI gui = new GUI(bot);
                bot.setGUI(gui);
                gui.init();
            }
            catch(Exception e)
            {
                LOG.error("No se pudo abrir la ventana. Si estás en un servidor sin pantalla, usa la opción -Dnogui=true.");
            }
        }

        EnumSet<GatewayIntent> intents = EnumSet.of(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MESSAGES);
        if(config.useTextResponses())
            intents.add(GatewayIntent.MESSAGE_CONTENT);

        AudioModuleConfig audioConfig = new AudioModuleConfig();
        try
        {
            audioConfig = audioConfig.withDaveSessionFactory(new JDaveSessionFactory());
        }
        catch(Throwable t)
        {
            LOG.error("No se pudo cargar el cifrado de voz DAVE (" + t + "). Sin DAVE el bot no puede entrar a canales de voz. "
                    + "Revisa que uses Java 25 de 64 bits en Windows, Linux o macOS.");
        }

        try
        {
            EnumSet<CacheFlag> disabled = EnumSet.allOf(CacheFlag.class);
            disabled.removeAll(EnumSet.of(CacheFlag.VOICE_STATE, CacheFlag.MEMBER_OVERRIDES));
            JDA jda = JDABuilder.create(config.getToken(), intents)
                    .setMemberCachePolicy(MemberCachePolicy.VOICE.or(MemberCachePolicy.OWNER))
                    .enableCache(CacheFlag.VOICE_STATE, CacheFlag.MEMBER_OVERRIDES)
                    .disableCache(disabled)
                    .setAudioModuleConfig(audioConfig)
                    .setEventPool(Executors.newVirtualThreadPerTaskExecutor(), true)
                    .setActivity(bot.getDefaultGame())
                    .setStatus(config.getStatus() == OnlineStatus.INVISIBLE || config.getStatus() == OnlineStatus.OFFLINE
                            ? OnlineStatus.INVISIBLE : config.getStatus())
                    .addEventListeners(commands, new Listener(bot))
                    .setBulkDeleteSplittingEnabled(true)
                    .build();
            bot.setJDA(jda);
            Runtime.getRuntime().addShutdownHook(new Thread(bot::cleanup, "tukibot-shutdown"));
            if(config.useTextResponses())
                jda.addEventListener(new TextResponses(bot));

            jda.awaitReady();
            checkApplication(jda, config);
            bot.startBackgroundTasks();
        }
        catch(InvalidTokenException ex)
        {
            prompt.alert(Prompt.Level.ERROR, "TukiBot", ex + "\nRevisa que estés editando el config.txt correcto y que el token sea "
                    + "el del bot (no el 'client secret').\nArchivo: " + config.getConfigLocation());
            System.exit(1);
        }
        catch(IllegalStateException ex)
        {
            // JDA lanza esto si Discord rechaza los intents (por ejemplo, MESSAGE CONTENT desactivado)
            prompt.alert(Prompt.Level.ERROR, "TukiBot", "Discord rechazó la conexión: " + ex.getMessage()
                    + "\nSi usas textresponses = true, activa 'MESSAGE CONTENT INTENT' en el Developer Portal o pon textresponses = false.");
            System.exit(1);
        }
        catch(IllegalArgumentException ex)
        {
            prompt.alert(Prompt.Level.ERROR, "TukiBot", "Hay un valor inválido en la configuración: " + ex
                    + "\nArchivo: " + config.getConfigLocation());
            System.exit(1);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }

    private static void checkApplication(JDA jda, BotConfig config)
    {
        try
        {
            ApplicationInfo info = jda.retrieveApplicationInfo().complete();
            if(config.getOwnerId() <= 0)
            {
                long owner = info.getTeam() != null ? info.getTeam().getOwnerIdLong() : info.getOwner().getIdLong();
                config.setOwnerId(owner);
                LOG.info("Dueño del bot tomado de la aplicación: " + owner);
            }
            if(info.isBotPublic())
                LOG.warn("Tu bot es público: cualquiera puede invitarlo. Si es solo para tus servidores, desactiva 'Public Bot' en "
                        + "https://discord.com/developers/applications/" + jda.getSelfUser().getId() + "/bot");
        }
        catch(Exception e)
        {
            LOG.warn("No se pudo leer la información de la aplicación: " + e);
        }
        LOG.info("Enlace para invitar al bot: " + jda.getInviteUrl(RECOMMENDED_PERMS));
    }

    private static CommandManager createCommandManager(Bot bot)
    {
        SearchCmd search = new SearchCmd(bot);
        PanelButtons panel = new PanelButtons(bot);
        return new CommandManager(bot)
                .addCommands(
                        // generales
                        new HelpCmd(bot),
                        new InfoCmd(bot),
                        new PingCmd(bot),
                        new SettingsCmd(bot),
                        new GenCmd(bot),
                        new CoinCmd(bot),
                        new TwitchCmd(bot),
                        // música
                        new PlayCmd(bot),
                        new PlayFileCmd(bot),
                        search,
                        new NowplayingCmd(bot),
                        new QueueCmd(bot),
                        new SkipCmd(bot),
                        new RemoveCmd(bot),
                        new SeekCmd(bot),
                        new ShuffleCmd(bot),
                        new LyricsCmd(bot),
                        new PlaylistCmd(bot),
                        new MyPlaylistCmd(bot),
                        // DJ
                        new PlaynextCmd(bot),
                        new PauseCmd(bot),
                        new ResumeCmd(bot),
                        new ForceskipCmd(bot),
                        new SkiptoCmd(bot),
                        new MoveTrackCmd(bot),
                        new ForceRemoveCmd(bot),
                        new RepeatCmd(bot),
                        new AutoplayCmd(bot),
                        new FilterCmd(bot),
                        new VolumeCmd(bot),
                        new StopCmd(bot),
                        // admin y dueño
                        new ConfigCmd(bot),
                        new OwnerCmd(bot))
                .addContextCommands(new PlayInVoiceContext(bot))
                .addComponentHandler("np", panel)
                .addComponentHandler("queue", panel)
                .addComponentHandler("search", search)
                .addComponentHandler("pl", new PlaylistPrompt(bot));
    }
}
