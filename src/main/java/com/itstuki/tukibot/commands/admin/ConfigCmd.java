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
package com.itstuki.tukibot.commands.admin;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.audio.AudioHandler;
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.music.Autocomplete;
import com.itstuki.tukibot.playlist.PlaylistLoader;
import com.itstuki.tukibot.settings.QueueType;
import com.itstuki.tukibot.settings.Settings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * Configuración del servidor. Junta setdj, settc, setvc, setskip, queuetype
 * y autoplaylist de JMusicBot. Se quitó "prefix" porque ahora todo es con
 * comandos slash.
 */
public class ConfigCmd extends SlashCommand
{
    public ConfigCmd(Bot bot)
    {
        super(bot);
        this.name = "config";
        this.nameEs = "config";
        this.help = "Configura el bot en este servidor";
        this.helpEn = "Configures the bot in this server";
        this.category = Category.ADMIN;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addSubcommands(
                Loc.sub("dj", "dj", "Rol que puede usar los comandos de DJ (vacío = quitarlo)", "Sets the DJ role (empty = clear)")
                        .addOptions(Loc.option(OptionType.ROLE, "role", "rol", "Rol de DJ", "DJ role", false)),
                Loc.sub("textchannel", "canal-texto", "Canal donde se permiten comandos de música (vacío = cualquiera)", "Sets the music text channel")
                        .addOptions(Loc.option(OptionType.CHANNEL, "channel", "canal", "Canal de texto", "Text channel", false)
                                .setChannelTypes(ChannelType.TEXT)),
                Loc.sub("voicechannel", "canal-voz", "Único canal de voz donde se puede poner música (vacío = cualquiera)", "Sets the music voice channel")
                        .addOptions(Loc.option(OptionType.CHANNEL, "channel", "canal", "Canal de voz", "Voice channel", false)
                                .setChannelTypes(ChannelType.VOICE)),
                Loc.sub("skipratio", "votos-salto", "Porcentaje de oyentes que debe votar para saltar", "Sets the skip percentage")
                        .addOptions(Loc.option(OptionType.INTEGER, "percent", "porcentaje", "Entre 0 y 100 (por defecto 55)", "0 - 100", true)
                                .setRequiredRange(0, 100)),
                Loc.sub("queuetype", "tipo-cola", "Cómo se ordena la cola", "Changes the queue type")
                        .addOptions(Loc.option(OptionType.STRING, "type", "tipo", "Tipo de cola", "Queue type", true)
                                .addChoice("Justa (se alternan las canciones de cada persona)", "FAIR")
                                .addChoice("Lineal (por orden de llegada)", "LINEAR")),
                Loc.sub("twitch", "twitch", "Canal donde se avisa cuando empieza el directo de Twitch (vacío = desactivar)", "Sets the Twitch live alert channel")
                        .addOptions(Loc.option(OptionType.CHANNEL, "channel", "canal", "Canal de texto o anuncios", "Text or announcement channel", false)
                                        .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS),
                                Loc.option(OptionType.ROLE, "role", "rol", "Rol a mencionar en el aviso (opcional)", "Role to ping (optional)", false)),
                Loc.sub("autoplaylist", "autoplaylist", "Playlist que suena cuando la cola está vacía (vacío = ninguna)", "Sets the default playlist")
                        .addOptions(Loc.option(OptionType.STRING, "name", "nombre", "Nombre de la playlist", "Playlist name", false).setAutoComplete(true)));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        Settings s = ctx.settings();
        String sub = ctx.event().getSubcommandName();
        switch(sub == null ? "" : sub)
        {
            case "dj" -> {
                OptionMapping o = ctx.option("role");
                Role role = o == null ? null : o.getAsRole();
                s.setDJRole(role);
                ctx.replySuccess(role == null
                        ? "Quité el rol de DJ; ahora solo los administradores pueden usar los comandos de DJ."
                        : "Los comandos de DJ ahora los pueden usar quienes tengan el rol " + role.getAsMention() + ".");
            }
            case "textchannel" -> {
                OptionMapping o = ctx.option("channel");
                TextChannel tc = o == null ? null : o.getAsChannel().asTextChannel();
                s.setTextChannel(tc);
                ctx.replySuccess(tc == null ? "Los comandos de música ahora se pueden usar en cualquier canal."
                        : "Los comandos de música ahora solo se pueden usar en " + tc.getAsMention() + ".");
            }
            case "voicechannel" -> {
                OptionMapping o = ctx.option("channel");
                VoiceChannel vc = o == null ? null : o.getAsChannel().asVoiceChannel();
                s.setVoiceChannel(vc);
                ctx.replySuccess(vc == null ? "Ahora se puede poner música en cualquier canal de voz."
                        : "Ahora solo se puede poner música en " + vc.getAsMention() + ".");
            }
            case "skipratio" -> {
                int val = (int) ctx.integer("percent", 55);
                s.setSkipRatio(val / 100.0);
                ctx.replySuccess("Ahora se necesita el `" + val + "%` de los oyentes para saltar una canción.");
            }
            case "queuetype" -> {
                QueueType value = QueueType.valueOf(ctx.string("type", "FAIR"));
                if(s.getQueueType() != value)
                {
                    s.setQueueType(value);
                    AudioHandler handler = bot.getPlayerManager().getHandler(ctx.guild());
                    if(handler != null)
                        handler.setQueueType(value);
                }
                ctx.reply(value.getEmoji() + " El tipo de cola ahora es **" + value.getUserFriendlyName() + "**.");
            }
            case "twitch" -> {
                OptionMapping o = ctx.option("channel");
                if(o == null)
                {
                    s.setTwitchNotify(0L, 0L);
                    ctx.replySuccess("Desactivé los avisos de directos de Twitch en este servidor.");
                    return;
                }
                GuildMessageChannel ch = o.getAsChannel().asGuildMessageChannel();
                OptionMapping r = ctx.option("role");
                Role role = r == null ? null : r.getAsRole();
                if(!ctx.guild().getSelfMember().hasPermission(ch, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND))
                {
                    ctx.replyError("No tengo permiso para escribir en " + ch.getAsMention() + ".");
                    return;
                }
                if(role != null && !role.isMentionable() && !ctx.guild().getSelfMember().hasPermission(ch, Permission.MESSAGE_MENTION_EVERYONE))
                {
                    ctx.replyError("No puedo mencionar a " + role.getAsMention() + ": activa *Permitir que cualquiera @mencione este rol* "
                            + "o dame el permiso *Mencionar @everyone, @here y todos los roles*.");
                    return;
                }
                s.setTwitchNotify(ch.getIdLong(), role == null ? 0L : role.getIdLong());
                String user = bot.getConfig().getTwitch();
                StringBuilder sb = new StringBuilder("Cuando **" + user + "** empiece un directo en Twitch lo avisaré en " + ch.getAsMention()
                        + (role == null ? "." : " mencionando a " + role.getAsMention() + "."));
                if(!bot.getTwitch().isEnabled())
                    sb.append("\n").append(bot.getConfig().getWarning()).append(" Ojo: el dueño del bot todavía no configuró `twitchnotify` en el config.txt, así que los avisos no saldrán hasta que lo haga.");
                ctx.replySuccess(sb.toString());
            }
            case "autoplaylist" -> {
                String pname = ctx.string("name", null);
                if(pname == null || pname.isBlank())
                {
                    s.setDefaultPlaylist(null);
                    ctx.replySuccess("Quité la playlist automática de **" + ctx.guild().getName() + "**.");
                    return;
                }
                pname = PlaylistLoader.sanitize(pname);
                if(bot.getPlaylistLoader().getPlaylist(pname) == null)
                    ctx.replyError("No encontré `" + pname + ".txt`.");
                else
                {
                    s.setDefaultPlaylist(pname);
                    ctx.replySuccess("La playlist automática de **" + ctx.guild().getName() + "** ahora es `" + pname + "`.");
                }
            }
            default -> ctx.replyError("Subcomando desconocido.");
        }
    }

    @Override
    public void autocomplete(CommandAutoCompleteInteractionEvent event)
    {
        Autocomplete.playlists(bot, event);
    }
}
