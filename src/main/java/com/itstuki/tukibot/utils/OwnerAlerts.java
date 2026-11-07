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
package com.itstuki.tukibot.utils;

import com.itstuki.tukibot.Bot;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Avisos por mensaje directo al dueño del bot (por ejemplo, cuando YouTube
 * bloquea la reproducción). Cada tipo de aviso tiene un tiempo de espera
 * para no llenarle el DM.
 */
public class OwnerAlerts
{
    private static final Logger LOG = LoggerFactory.getLogger("Avisos");

    private final Bot bot;
    private final Map<String, Long> lastSent = new ConcurrentHashMap<>();

    public OwnerAlerts(Bot bot)
    {
        this.bot = bot;
    }

    /**
     * @param key        tipo de aviso (para el tiempo de espera)
     * @param message    texto del aviso
     * @param cooldownMs tiempo mínimo entre dos avisos del mismo tipo
     */
    public void send(String key, String message, long cooldownMs)
    {
        long now = System.currentTimeMillis();
        Long last = lastSent.get(key);
        if(last != null && now - last < cooldownMs)
            return;
        lastSent.put(key, now);
        LOG.warn(message.replace("\n", " "));
        if(bot.getJDA() == null || bot.getConfig().getOwnerId() <= 0)
            return;
        bot.getJDA().retrieveUserById(bot.getConfig().getOwnerId())
                .flatMap(user -> user.openPrivateChannel())
                .flatMap(channel -> channel.sendMessage(FormatUtil.truncate(message, 2000)))
                .queue(s -> {}, f -> LOG.warn("No pude enviarle el aviso por DM al dueño (¿tiene los DM cerrados?): " + f.getMessage()));
    }
}
