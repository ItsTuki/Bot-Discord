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
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

/** Respuesta a "ItsTuki" del bot original, ahora con botón de enlace. */
public class TwitchCmd extends SlashCommand
{
    public TwitchCmd(Bot bot)
    {
        super(bot);
        this.name = "twitch";
        this.nameEs = "twitch";
        this.help = "Comparte el canal de Twitch de ItsTuki";
        this.helpEn = "Shares ItsTuki's Twitch channel";
        this.guildOnly = false;
    }

    public static String url(Bot bot)
    {
        return "https://www.twitch.tv/" + bot.getConfig().getTwitch();
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        var notifier = bot.getTwitch();
        var stream = notifier.isEnabled() ? notifier.getCurrent() : null;
        if(stream != null)
        {
            ctx.reply(notifier.message(ctx.guild(), stream, null), false, null);
            return;
        }
        String status = notifier.isEnabled() && notifier.isWorking() ? "\n-# Ahora mismo está desconectado." : "";
        ctx.reply(new MessageCreateBuilder()
                .setContent("🟣 Puedes seguirme en Twitch: " + url(bot) + status)
                .setComponents(ActionRow.of(Button.link(url(bot), "Ir a Twitch")))
                .build(), false, null);
    }
}
