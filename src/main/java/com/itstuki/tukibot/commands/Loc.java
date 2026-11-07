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
package com.itstuki.tukibot.commands;

import java.util.EnumMap;
import java.util.Map;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

/**
 * Localización de comandos: los nombres base están en inglés (como en
 * JMusicBot) y los clientes de Discord en español ven los nombres en
 * español. Las descripciones base están en español y se traducen al inglés.
 */
public final class Loc
{
    private Loc() {}

    public static Map<DiscordLocale, String> es(String value)
    {
        Map<DiscordLocale, String> map = new EnumMap<>(DiscordLocale.class);
        map.put(DiscordLocale.SPANISH, value);
        map.put(DiscordLocale.SPANISH_LATAM, value);
        return map;
    }

    public static Map<DiscordLocale, String> en(String value)
    {
        Map<DiscordLocale, String> map = new EnumMap<>(DiscordLocale.class);
        map.put(DiscordLocale.ENGLISH_US, value);
        map.put(DiscordLocale.ENGLISH_UK, value);
        return map;
    }

    /** Opción con nombre en inglés/español y descripción en español/inglés. */
    public static OptionData option(OptionType type, String name, String nameEs, String desc, String descEn, boolean required)
    {
        OptionData o = new OptionData(type, name, desc, required);
        if(nameEs != null && !nameEs.equals(name))
            o.setNameLocalizations(es(nameEs));
        if(descEn != null)
            o.setDescriptionLocalizations(en(descEn));
        return o;
    }

    public static SubcommandData sub(String name, String nameEs, String desc, String descEn)
    {
        SubcommandData s = new SubcommandData(name, desc);
        if(nameEs != null && !nameEs.equals(name))
            s.setNameLocalizations(es(nameEs));
        if(descEn != null)
            s.setDescriptionLocalizations(en(descEn));
        return s;
    }
}
