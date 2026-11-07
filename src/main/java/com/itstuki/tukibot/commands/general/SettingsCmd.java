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
package com.itstuki.tukibot.commands.general;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.settings.QueueType;
import com.itstuki.tukibot.settings.RepeatMode;
import com.itstuki.tukibot.settings.Settings;
import com.itstuki.tukibot.ui.Panels;
import java.util.List;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

public class SettingsCmd extends SlashCommand
{
    public SettingsCmd(Bot bot)
    {
        super(bot);
        this.name = "settings";
        this.nameEs = "ajustes";
        this.help = "Muestra la configuración del bot en este servidor";
        this.helpEn = "Shows the bot settings";
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        Settings s = ctx.settings();
        TextChannel tchan = s.getTextChannel(ctx.guild());
        VoiceChannel vchan = s.getVoiceChannel(ctx.guild());
        Role role = s.getRole(ctx.guild());
        double ratio = s.getSkipRatio() == -1 ? bot.getConfig().getSkipRatio() : s.getSkipRatio();
        String body = "Canal de texto: " + (tchan == null ? "Cualquiera" : tchan.getAsMention())
                + "\nCanal de voz: " + (vchan == null ? "Cualquiera" : vchan.getAsMention())
                + "\nRol de DJ: " + (role == null ? "Ninguno" : role.getAsMention())
                + "\nRepetición: " + (s.getRepeatMode() == RepeatMode.OFF ? s.getRepeatMode().getUserFriendlyName() : "**" + s.getRepeatMode().getUserFriendlyName() + "**")
                + "\nTipo de cola: " + (s.getQueueType() == QueueType.FAIR ? s.getQueueType().getUserFriendlyName() : "**" + s.getQueueType().getUserFriendlyName() + "**")
                + "\nVotos para saltar: `" + Math.round(ratio * 100) + "%`"
                + "\nVolumen: `" + s.getVolume() + "`"
                + "\nPlaylist automática: " + (s.getDefaultPlaylist() == null ? "Ninguna" : "**" + s.getDefaultPlaylist() + "**");
        var jda = ctx.event().getJDA();
        long audio = jda.getGuilds().stream().filter(g -> g.getSelfMember().getVoiceState() != null && g.getSelfMember().getVoiceState().inAudioChannel()).count();
        ctx.reply(Panels.container(bot, ctx.guild(), List.of(
                TextDisplay.of("### 🎧 Ajustes de " + jda.getSelfUser().getName() + "\n" + body),
                Separator.createDivider(Separator.Spacing.SMALL),
                TextDisplay.of("-# Cámbialos con " + bot.getCommands().mention("config") + " · " + jda.getGuilds().size() + " servidores · "
                        + audio + " conexiones de audio"))), false, null);
    }
}
