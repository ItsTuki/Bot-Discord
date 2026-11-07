/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 * Copyright 2026 ItsTuki (migración a JDA 6).
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

import com.itstuki.tukibot.TukiBot;
import com.itstuki.tukibot.entities.Prompt;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

public class OtherUtil
{
    private final static String WINDOWS_INVALID_PATH = "c:\\windows\\system32\\";

    /**
     * Devuelve una ruta relativa a la carpeta del bot. Evita que en Windows se
     * terminen escribiendo archivos en system32 cuando se lanza con doble clic.
     */
    public static Path getPath(String path)
    {
        Path result = Paths.get(path);
        if(result.toAbsolutePath().toString().toLowerCase().startsWith(WINDOWS_INVALID_PATH))
        {
            try
            {
                result = Paths.get(new File(TukiBot.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getPath() + File.separator + path);
            }
            catch(URISyntaxException ignored) {}
        }
        return result;
    }

    public static String loadResource(Object clazz, String name)
    {
        InputStream in = clazz.getClass().getResourceAsStream(name);
        if(in == null)
            return null;
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
        {
            StringBuilder sb = new StringBuilder();
            reader.lines().forEach(line -> sb.append("\n").append(line));
            return sb.toString().trim();
        }
        catch(IOException ignored)
        {
            return null;
        }
    }

    public static InputStream imageFromUrl(String url)
    {
        if(url==null)
            return null;
        try
        {
            URLConnection urlConnection = URI.create(url).toURL().openConnection();
            urlConnection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36");
            return urlConnection.getInputStream();
        }
        catch(IOException | IllegalArgumentException ignore) {}
        return null;
    }

    /**
     * Interpreta el texto de "game" del config. Acepta los prefijos en inglés
     * de JMusicBot (playing, listening, watching, streaming, competing) y sus
     * equivalentes en español (jugando, escuchando, viendo, transmitiendo, compitiendo).
     */
    public static Activity parseGame(String game)
    {
        if(game==null || game.trim().isEmpty() || game.trim().equalsIgnoreCase("default"))
            return null;
        String g = game.trim();
        String lower = g.toLowerCase();
        String[][] map = {
            {"listening to", "listening"}, {"listening", "listening"}, {"escuchando", "listening"},
            {"playing", "playing"}, {"jugando", "playing"},
            {"watching", "watching"}, {"viendo", "watching"},
            {"competing in", "competing"}, {"competing", "competing"}, {"compitiendo en", "competing"}, {"compitiendo", "competing"},
            {"streaming", "streaming"}, {"transmitiendo", "streaming"}
        };
        for(String[] entry : map)
        {
            if(!lower.startsWith(entry[0]))
                continue;
            String rest = g.substring(entry[0].length()).trim();
            switch(entry[1])
            {
                case "listening": return Activity.listening(makeNonEmpty(rest));
                case "playing": return Activity.playing(makeNonEmpty(rest));
                case "watching": return Activity.watching(makeNonEmpty(rest));
                case "competing": return Activity.competing(makeNonEmpty(rest));
                case "streaming":
                    String[] parts = rest.split("\\s+", 2);
                    if(parts.length == 2)
                        return Activity.streaming(makeNonEmpty(parts[1]), "https://twitch.tv/"+parts[0]);
                    break;
                default:
            }
        }
        return Activity.customStatus(g);
    }

    public static String makeNonEmpty(String str)
    {
        return str == null || str.isEmpty() ? "\u200B" : str;
    }

    public static OnlineStatus parseStatus(String status)
    {
        if(status==null || status.trim().isEmpty())
            return OnlineStatus.ONLINE;
        OnlineStatus st = OnlineStatus.fromKey(status);
        return st == null || st == OnlineStatus.UNKNOWN ? OnlineStatus.ONLINE : st;
    }

    public static void checkJavaVersion(Prompt prompt)
    {
        if(Runtime.version().feature() < 25)
            prompt.alert(Prompt.Level.ERROR, "Versión de Java",
                    "TukiBot necesita Java 25 o superior (lo exige el cifrado de voz DAVE). Versión actual: " + Runtime.version());
        else if(!System.getProperty("java.vm.name").contains("64") && !System.getProperty("os.arch").contains("64"))
            prompt.alert(Prompt.Level.WARNING, "Versión de Java",
                    "Parece que no estás usando Java de 64 bits. El audio de voz (DAVE) solo funciona en 64 bits.");
    }

    public static String getCurrentVersion()
    {
        if(TukiBot.class.getPackage()!=null && TukiBot.class.getPackage().getImplementationVersion()!=null)
            return TukiBot.class.getPackage().getImplementationVersion();
        else
            return "DEV";
    }
}
