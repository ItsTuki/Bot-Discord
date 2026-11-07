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
package com.itstuki.tukibot.interactions;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.commands.general.TwitchCmd;
import com.itstuki.tukibot.fun.BotLogic;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Respuestas de texto del bot original en Python (main.py), para quienes
 * todavía las usan sin barra. Se activa con textresponses = true.
 * El "modo eco" (repetir cualquier mensaje) se quitó porque llenaba los
 * canales; para todo lo demás están los comandos slash.
 */
public class TextResponses extends ListenerAdapter
{
    private final Bot bot;

    public TextResponses(Bot bot)
    {
        this.bot = bot;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if(event.getAuthor().isBot() || event.isWebhookMessage())
            return;
        String content = event.getMessage().getContentRaw();
        if(content.startsWith("ItsTuki"))
            event.getChannel().sendMessage("Puedes seguirme en Twitch: " + TwitchCmd.url(bot)).queue();
        else if(content.startsWith("$bye"))
            event.getChannel().sendMessage("🙂").queue();
        else if(content.startsWith("!gen"))
            event.getChannel().sendMessage("La clave generada es: ||`" + BotLogic.genPass(10) + "`||\n-# Usa /gen para que solo tú la veas.").queue();
        else if(content.startsWith("!moneda"))
            event.getChannel().sendMessage(BotLogic.flipCoin()).queue();
    }
}
