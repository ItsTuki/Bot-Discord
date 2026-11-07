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
import com.itstuki.tukibot.ui.Panels;
import com.itstuki.tukibot.utils.OtherUtil;
import java.util.List;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;

/** Reemplaza al AboutCommand de JDA-Utilities. */
public class InfoCmd extends SlashCommand
{
    public InfoCmd(Bot bot)
    {
        super(bot);
        this.name = "about";
        this.nameEs = "info";
        this.help = "Información sobre el bot";
        this.helpEn = "Information about the bot";
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        var jda = ctx.event().getJDA();
        String text = "## " + jda.getSelfUser().getName() + "\n"
                + "Bot de música de **ItsTuki** basado en [JMusicBot](https://github.com/jagrosh/MusicBot) de jagrosh, "
                + "actualizado para el Discord actual: comandos slash, botones, menús, paneles y voz cifrada (DAVE).\n"
                + "• Música de alta calidad (YouTube, SoundCloud, Bandcamp, Twitch, archivos...)\n"
                + "• Cola justa: se alternan las canciones de cada persona\n"
                + "• Fácil de hospedar uno mismo";
        String footer = "-# Versión " + OtherUtil.getCurrentVersion() + " · JDA " + JDAInfo.VERSION
                + " · " + jda.getGuildCache().size() + " servidores · Dueño: <@" + bot.getConfig().getOwnerId() + ">";
        ctx.reply(Panels.container(bot, ctx.guild(), List.of(
                Section.of(Thumbnail.fromUrl(jda.getSelfUser().getEffectiveAvatarUrl()), TextDisplay.of(text)),
                Separator.createDivider(Separator.Spacing.SMALL),
                TextDisplay.of(footer),
                ActionRow.of(
                        Button.link("https://github.com/ItsTuki/Bot-Discord", "Código fuente"),
                        Button.link(TwitchCmd.url(bot), "Twitch de ItsTuki")))), false, null);
    }
}
