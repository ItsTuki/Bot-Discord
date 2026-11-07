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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Guarda por un rato datos asociados a botones o menús (resultados de
 * búsqueda, playlists pendientes, etc.). Reemplaza al EventWaiter de
 * JDA-Utilities: el ID del componente lleva una clave corta y los datos
 * viven aquí hasta que expiran.
 */
public class InteractionCache
{
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(System.currentTimeMillis() % 100000);

    public InteractionCache(ScheduledExecutorService scheduler)
    {
        scheduler.scheduleWithFixedDelay(() ->
                entries.entrySet().removeIf(e -> e.getValue().expires < System.currentTimeMillis()),
                1, 1, TimeUnit.MINUTES);
    }

    /** Guarda un valor y devuelve la clave para usar en el custom ID. */
    public String put(Object value, long ownerId, long ttlSeconds)
    {
        String key = Long.toString(counter.incrementAndGet(), 36);
        entries.put(key, new Entry(value, ownerId, System.currentTimeMillis() + ttlSeconds * 1000));
        return key;
    }

    public Entry get(String key)
    {
        Entry e = entries.get(key);
        if(e == null || e.expires < System.currentTimeMillis())
        {
            entries.remove(key);
            return null;
        }
        return e;
    }

    public void remove(String key)
    {
        entries.remove(key);
    }

    public record Entry(Object value, long ownerId, long expires)
    {
        @SuppressWarnings("unchecked")
        public <T> T as()
        {
            return (T) value;
        }
    }
}
