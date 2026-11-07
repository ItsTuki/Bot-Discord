/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 * Copyright 2026 ItsTuki (reescrito para interacciones de JDA 6).
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
package com.itstuki.tukibot.commands;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.audio.AudioHandler;
import com.itstuki.tukibot.settings.Settings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;

/**
 * Comprobaciones de permisos y de voz que JMusicBot hacía en
 * MusicCommand, DJCommand y AdminCommand.
 */
public final class Checks
{
    private Checks() {}

    public static boolean isOwner(Bot bot, User user)
    {
        return user.getIdLong() == bot.getConfig().getOwnerId();
    }

    public static boolean isAdmin(Bot bot, Member member)
    {
        return isOwner(bot, member.getUser()) || member.hasPermission(Permission.MANAGE_SERVER);
    }

    public static boolean isDJ(Bot bot, Member member)
    {
        if(isAdmin(bot, member))
            return true;
        Settings settings = bot.getSettingsManager().getSettings(member.getGuild());
        Role dj = settings.getRole(member.getGuild());
        return dj != null && (member.getRoles().contains(dj) || dj.getIdLong() == member.getGuild().getIdLong());
    }

    /**
     * Revisa las condiciones para usar comandos de música.
     *
     * @return null si todo está bien, o el mensaje de error para el usuario
     */
    public static String checkMusic(Bot bot, Member member, GuildMessageChannel channel, boolean bePlaying, boolean beListening)
    {
        Guild guild = member.getGuild();
        Settings settings = bot.getSettingsManager().getSettings(guild);
        TextChannel tchannel = settings.getTextChannel(guild);
        if(tchannel != null && channel != null && channel.getIdLong() != tchannel.getIdLong())
            return "Los comandos de música solo se pueden usar en " + tchannel.getAsMention() + ".";

        AudioHandler handler = bot.getPlayerManager().setUpHandler(guild);
        if(bePlaying && !handler.isMusicPlaying(guild.getJDA()))
            return "Tiene que haber música sonando para usar eso.";

        if(beListening)
        {
            GuildVoiceState selfState = guild.getSelfMember().getVoiceState();
            AudioChannel current = selfState == null ? null : selfState.getChannel();
            if(current == null)
                current = settings.getVoiceChannel(guild);
            GuildVoiceState userState = member.getVoiceState();
            if(userState == null || !userState.inAudioChannel() || userState.isDeafened()
                    || (current != null && !userState.getChannel().equals(current)))
                return "Tienes que estar escuchando en " + (current == null ? "un canal de voz" : current.getAsMention()) + " para usar eso.";

            AudioChannel afk = guild.getAfkChannel();
            if(afk != null && afk.equals(userState.getChannel()))
                return "No puedo usar comandos de música en el canal AFK.";

            if(selfState == null || !selfState.inAudioChannel())
            {
                AudioChannel target = userState.getChannel();
                if(!guild.getSelfMember().hasPermission(target, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK))
                    return "No tengo permiso para entrar o hablar en " + target.getAsMention() + ".";
                try
                {
                    guild.getAudioManager().openAudioConnection(target);
                    watchConnection(bot, guild);
                }
                catch(PermissionException ex)
                {
                    return "No puedo conectarme a " + target.getAsMention() + " (¿está lleno?).";
                }
            }
        }
        return null;
    }

    /**
     * Si la conexión de voz no se completa en 20 segundos (firewall, UDP
     * bloqueado, región caída...), el bot sale y avisa en vez de quedarse
     * reintentando para siempre.
     */
    public static void watchConnection(Bot bot, Guild guild)
    {
        bot.getThreadpool().schedule(() -> {
            var am = guild.getAudioManager();
            if(am.isConnected() || am.getConnectionStatus() == net.dv8tion.jda.api.audio.hooks.ConnectionStatus.NOT_CONNECTED)
                return;
            AudioHandler handler = bot.getPlayerManager().getHandler(guild);
            bot.getNowplayingHandler().announce(guild.getIdLong(), bot.getConfig().getError()
                    + " No pude conectarme al canal de voz después de 20 segundos, así que me salí."
                    + "\n-# Si se repite, revisa que el equipo donde corre el bot no bloquee las conexiones UDP salientes.");
            if(handler != null)
                handler.stopAndClear();
            am.closeAudioConnection();
        }, 20, java.util.concurrent.TimeUnit.SECONDS);
    }
}
