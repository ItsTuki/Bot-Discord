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
package com.itstuki.tukibot.fun;

import java.security.SecureRandom;

/**
 * Funciones del primer bot de ItsTuki (bot_logic.py), pasadas a Java.
 */
public final class BotLogic
{
    // mismo alfabeto que la versión en Python (ahora incluye la "m" que faltaba)
    private static final String ELEMENTS = "+-/*!&$#?=@abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    private static final SecureRandom RANDOM = new SecureRandom();

    private BotLogic() {}

    /** gen_pass(): contraseña aleatoria (ahora con un generador seguro). */
    public static String genPass(int length)
    {
        StringBuilder sb = new StringBuilder(length);
        for(int i = 0; i < length; i++)
            sb.append(ELEMENTS.charAt(RANDOM.nextInt(ELEMENTS.length())));
        return sb.toString();
    }

    /** flip_coin(): "Cara" o "Cruz". */
    public static String flipCoin()
    {
        return RANDOM.nextBoolean() ? "Cara" : "Cruz";
    }
}
