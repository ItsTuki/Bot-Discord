/*
 * Copyright 2016 John Grosh (john.a.grosh@gmail.com).
 * Copyright 2026 ItsTuki (migración a JDA 6 y registro de comandos slash).
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

import com.itstuki.tukibot.audio.AudioHandler;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Listener extends ListenerAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger("TukiBot");
    private volatile boolean ready = false;
    private final Bot bot;

    public Listener(Bot bot)
    {
        this.bot = bot;
    }

    @Override
    public void onReady(ReadyEvent event)
    {
        var jda = event.getJDA();
        ready = true;
        LOG.info("Conectado como " + jda.getSelfUser().getName() + " en " + jda.getGuildCache().size() + " servidores.");
        if(jda.getGuildCache().isEmpty())
        {
            LOG.warn("El bot no está en ningún servidor. Invítalo con este enlace:");
            LOG.warn(jda.getInviteUrl(TukiBot.RECOMMENDED_PERMS));
        }

        bot.getCommands().upsertCommands(jda);

        jda.getGuilds().forEach(guild ->
        {
            try
            {
                String defpl = bot.getSettingsManager().getSettings(guild).getDefaultPlaylist();
                VoiceChannel vc = bot.getSettingsManager().getSettings(guild).getVoiceChannel(guild);
                if(defpl != null && vc != null && bot.getPlayerManager().setUpHandler(guild).playFromDefault())
                {
                    guild.getAudioManager().openAudioConnection(vc);
                    com.itstuki.tukibot.commands.Checks.watchConnection(bot, guild);
                }
            }
            catch(Exception ignore) {}
        });
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event)
    {
        if(event.isFromGuild())
            bot.getNowplayingHandler().onMessageDelete(event.getGuild(), event.getMessageIdLong());
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event)
    {
        bot.getAloneInVoiceHandler().onVoiceUpdate(event);
        // si al bot lo desconectaron a mano, se limpia la cola
        if(event.getMember().getIdLong() == event.getJDA().getSelfUser().getIdLong() && event.getChannelJoined() == null)
        {
            AudioHandler handler = bot.getPlayerManager().getHandler(event.getGuild());
            if(handler != null && handler.getPlayer().getPlayingTrack() != null)
            {
                handler.stopAndClear();
                bot.getNowplayingHandler().refresh(event.getGuild());
            }
        }
    }

    @Override
    public void onShutdown(ShutdownEvent event)
    {
        if(!ready)
            LOG.error("No se pudo iniciar sesión en Discord. Lo más probable es que el token sea inválido: "
                    + "revisa config.txt o la variable BOT_TOKEN (Developer Portal > Bot > Reset Token).");
        else if(event.getCloseCode() != null && !event.getCloseCode().isReconnect())
            LOG.error("Discord cerró la conexión: " + event.getCloseCode().getMeaning()
                    + (event.getCloseCode() == net.dv8tion.jda.api.requests.CloseCode.AUTHENTICATION_FAILED
                        ? " Revisa el token (en config.txt o en la variable BOT_TOKEN)." : ""));
        bot.shutdown();
    }
}
