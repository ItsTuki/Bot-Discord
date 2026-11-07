/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
 * Copyright 2026 ItsTuki (migración a JDA 6 y textos en español).
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

import com.itstuki.tukibot.audio.RequestMetadata.UserInfo;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.entities.User;

/**
 * Utilidades de formato para los mensajes del bot.
 */
public class FormatUtil
{
    public static String formatUsername(UserInfo userinfo)
    {
        return userinfo == null ? "desconocido" : userinfo.username;
    }

    public static String formatUsername(User user)
    {
        return user.getEffectiveName();
    }

    public static String progressBar(double percent)
    {
        StringBuilder str = new StringBuilder();
        for(int i=0; i<14; i++)
            if(i == (int)(percent*14))
                str.append("🔘"); // 🔘
            else
                str.append("▬");
        return str.toString();
    }

    public static String volumeIcon(int volume)
    {
        if(volume == 0)
            return "🔇"; // 🔇
        if(volume < 30)
            return "🔈"; // 🔈
        if(volume < 70)
            return "🔉"; // 🔉
        return "🔊";     // 🔊
    }

    /** Escapa el texto para que no rompa el markdown de Discord. */
    public static String escape(String input)
    {
        if(input == null)
            return "";
        return filter(input.replace("\\", "\\\\").replace("*", "\\*").replace("_", "\\_")
                .replace("~", "\\~").replace("`", "\\`").replace("|", "\\|")
                .replace("[", "\\[").replace("]", "\\]").replace("#", "\\#"));
    }

    /** Título de la pista como enlace markdown (si tiene URL). */
    public static String trackLink(AudioTrack track)
    {
        AudioTrackInfo info = track.getInfo();
        String title = escape(truncate(info.title, 90));
        return info.uri != null && info.uri.startsWith("http") ? "[" + title + "](<" + info.uri + ">)" : title;
    }

    public static String truncate(String input, int max)
    {
        if(input == null)
            return "";
        return input.length() <= max ? input : input.substring(0, Math.max(0, max - 1)) + "…";
    }

    public static String filter(String input)
    {
        return input.replace("‮","")
                .replace("@everyone", "@еveryone") // letra e cirílica
                .replace("@here", "@hеre") // letra e cirílica
                .trim();
    }
}
