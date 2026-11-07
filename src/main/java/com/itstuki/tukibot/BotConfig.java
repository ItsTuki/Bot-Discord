/*
 * Copyright 2018 John Grosh (jagrosh).
 * Copyright 2026 ItsTuki (opciones nuevas para JDA 6, YouTube y comandos slash).
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
package com.itstuki.tukibot;

import com.itstuki.tukibot.entities.Prompt;
import com.itstuki.tukibot.utils.OtherUtil;
import com.itstuki.tukibot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.typesafe.config.*;
import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

/**
 * Lee el archivo config.txt (formato HOCON) con valores por defecto desde
 * reference.conf.
 */
public class BotConfig
{
    private final Prompt prompt;
    private final static String CONTEXT = "Config";
    private final static String START_TOKEN = "/// START OF TUKIBOT CONFIG ///";
    private final static String END_TOKEN = "/// END OF TUKIBOT CONFIG ///";

    private Path path = null;
    private String token, playlistsFolder, logLevel, twitch,
            successEmoji, warningEmoji, errorEmoji, loadingEmoji, searchingEmoji,
            ytRefreshToken, ytRemoteCipher, ytRemoteCipherPassword, ytdlpPath, ytCookies, ytCookiesBrowser;
    private boolean stayInChannel, songInGame, npImages, vcStatus, npAuto, autocomplete,
            textResponses, ytOauth, ytdlp;
    private long owner, maxSeconds, aloneTimeUntilStop, devGuild;
    private int maxYTPlaylistPages;
    private double skipratio;
    private Color color;
    private OnlineStatus status;
    private Activity game;
    private boolean gameNone;
    private Config transforms;

    private boolean valid = false;

    public BotConfig(Prompt prompt)
    {
        this.prompt = prompt;
    }

    public void load()
    {
        valid = false;
        try
        {
            path = getConfigPath();
            Config config = ConfigFactory.load();

            token = config.getString("token").trim();
            owner = config.getLong("owner");
            successEmoji = config.getString("success");
            warningEmoji = config.getString("warning");
            errorEmoji = config.getString("error");
            loadingEmoji = config.getString("loading");
            searchingEmoji = config.getString("searching");
            String gameStr = config.getString("game");
            gameNone = gameStr.trim().equalsIgnoreCase("none");
            game = gameNone ? null : OtherUtil.parseGame(gameStr);
            status = OtherUtil.parseStatus(config.getString("status"));
            stayInChannel = config.getBoolean("stayinchannel");
            songInGame = config.getBoolean("songinstatus");
            vcStatus = config.getBoolean("vcstatus");
            npAuto = config.getBoolean("npauto");
            autocomplete = config.getBoolean("autocomplete");
            textResponses = config.getBoolean("textresponses");
            twitch = config.getString("twitch").trim();
            devGuild = config.getLong("devguild");
            npImages = config.getBoolean("npimages");
            logLevel = config.getString("loglevel");
            maxSeconds = config.getLong("maxtime");
            maxYTPlaylistPages = config.getInt("maxytplaylistpages");
            aloneTimeUntilStop = config.getLong("alonetimeuntilstop");
            playlistsFolder = config.getString("playlistsfolder");
            transforms = config.getConfig("transforms");
            skipratio = config.getDouble("skipratio");
            color = parseColor(config.getString("color"));
            ytOauth = config.getBoolean("youtube.oauth");
            ytRefreshToken = config.getString("youtube.refreshtoken").trim();
            ytRemoteCipher = config.getString("youtube.remotecipher").trim();
            ytRemoteCipherPassword = config.getString("youtube.remotecipherpassword").trim();
            ytdlp = config.getBoolean("youtube.ytdlp");
            ytdlpPath = config.getString("youtube.ytdlppath").trim();
            ytCookies = config.getString("youtube.cookies").trim();
            ytCookiesBrowser = config.getString("youtube.cookiesfrombrowser").trim();

            boolean write = false;

            // validar el token
            if(token.isEmpty() || token.equalsIgnoreCase("BOT_TOKEN_HERE"))
            {
                token = prompt.prompt("Falta el token del bot."
                        + "\nLo obtienes en https://discord.com/developers/applications > tu aplicación > Bot > Reset Token."
                        + "\nToken del bot: ");
                if(token==null || token.isBlank())
                {
                    prompt.alert(Prompt.Level.WARNING, CONTEXT, "No se ingresó un token. Saliendo.\n\nArchivo de configuración: " + path.toAbsolutePath());
                    return;
                }
                token = token.trim();
                write = true;
            }

            if(write)
                writeToFile();

            valid = true;
        }
        catch (ConfigException ex)
        {
            prompt.alert(Prompt.Level.ERROR, CONTEXT, ex + ": " + ex.getMessage() + "\n\nArchivo de configuración: " + path.toAbsolutePath());
        }
    }

    private static Color parseColor(String value)
    {
        if(value == null || value.isBlank() || value.equalsIgnoreCase("default"))
            return null;
        try
        {
            return Color.decode(value.trim().startsWith("#") ? value.trim() : "#" + value.trim());
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }

    private void writeToFile()
    {
        String content = Files.exists(path) ? readFile(path) : null;
        if(content == null || !content.contains("BOT_TOKEN_HERE"))
            content = loadDefaultConfig();
        byte[] bytes = content.replace("BOT_TOKEN_HERE", token).trim().getBytes(StandardCharsets.UTF_8);
        try
        {
            Files.write(path, bytes);
        }
        catch(IOException ex)
        {
            prompt.alert(Prompt.Level.WARNING, CONTEXT, "No se pudo guardar config.txt: "+ex
                + "\nRevisa que la carpeta no esté protegida (por ejemplo, el escritorio en algunos sistemas).\n\nArchivo de configuración: "
                + path.toAbsolutePath());
        }
    }

    private static String readFile(Path p)
    {
        try
        {
            return Files.readString(p, StandardCharsets.UTF_8);
        }
        catch(IOException e)
        {
            return null;
        }
    }

    private static String loadDefaultConfig()
    {
        String original = OtherUtil.loadResource(new TukiBot(), "/reference.conf");
        return original==null
                ? "token = BOT_TOKEN_HERE\nowner = 0"
                : original.substring(original.indexOf(START_TOKEN)+START_TOKEN.length(), original.indexOf(END_TOKEN)).trim();
    }

    private static Path getConfigPath()
    {
        Path path = OtherUtil.getPath(System.getProperty("config.file", System.getProperty("config", "config.txt")));
        if(path.toFile().exists())
        {
            if(System.getProperty("config.file") == null)
                System.setProperty("config.file", System.getProperty("config", path.toAbsolutePath().toString()));
            ConfigFactory.invalidateCaches();
        }
        return path;
    }

    public static void writeDefaultConfig()
    {
        Prompt prompt = new Prompt(null, null, true, true);
        prompt.alert(Prompt.Level.INFO, "TukiBot Config", "Generando archivo de configuración por defecto");
        Path path = BotConfig.getConfigPath();
        byte[] bytes = BotConfig.loadDefaultConfig().getBytes(StandardCharsets.UTF_8);
        try
        {
            prompt.alert(Prompt.Level.INFO, "TukiBot Config", "Escribiendo configuración en " + path.toAbsolutePath());
            Files.write(path, bytes);
        }
        catch(Exception ex)
        {
            prompt.alert(Prompt.Level.ERROR, "TukiBot Config", "Error al escribir la configuración: " + ex.getMessage());
        }
    }

    public boolean isValid() { return valid; }
    public String getConfigLocation() { return path.toFile().getAbsolutePath(); }
    public String getToken() { return token; }
    public double getSkipRatio() { return skipratio; }
    public long getOwnerId() { return owner; }
    /** Se usa cuando owner = 0 para tomar el dueño de la aplicación. */
    public void setOwnerId(long owner) { this.owner = owner; }
    public String getSuccess() { return successEmoji; }
    public String getWarning() { return warningEmoji; }
    public String getError() { return errorEmoji; }
    public String getLoading() { return loadingEmoji; }
    public String getSearching() { return searchingEmoji; }
    public Activity getGame() { return game; }
    public boolean isGameNone() { return gameNone; }
    public OnlineStatus getStatus() { return status; }
    public boolean getStay() { return stayInChannel; }
    public boolean getSongInStatus() { return songInGame; }
    public boolean useVoiceStatus() { return vcStatus; }
    public boolean useAutoPanel() { return npAuto; }
    public boolean useAutocomplete() { return autocomplete; }
    public boolean useTextResponses() { return textResponses; }
    public String getTwitch() { return twitch; }
    public long getDevGuild() { return devGuild; }
    public Color getColor() { return color; }
    public String getPlaylistsFolder() { return playlistsFolder; }
    public String getLogLevel() { return logLevel; }
    public boolean useNPImages() { return npImages; }
    public long getMaxSeconds() { return maxSeconds; }
    public int getMaxYTPlaylistPages() { return maxYTPlaylistPages; }
    public String getMaxTime() { return TimeUtil.formatTime(maxSeconds * 1000); }
    public long getAloneTimeUntilStop() { return aloneTimeUntilStop; }
    public boolean useYoutubeOauth() { return ytOauth; }
    public String getYoutubeRefreshToken() { return ytRefreshToken.isEmpty() ? null : ytRefreshToken; }
    public boolean useYtDlp() { return ytdlp; }
    public String getYtDlpPath() { return ytdlpPath.isEmpty() ? null : ytdlpPath; }
    public String getYoutubeCookies() { return ytCookies.isEmpty() ? null : ytCookies; }
    public String getYoutubeCookiesBrowser() { return ytCookiesBrowser.isEmpty() ? null : ytCookiesBrowser; }

    /** Servidor yt-cipher público recomendado por los autores de youtube-source. */
    public static final String DEFAULT_REMOTE_CIPHER = "https://cipher.kikkia.dev/";

    /**
     * Vacío o "default" = servidor público; "none" = descifrar localmente
     * (suele romperse cada vez que YouTube cambia su reproductor).
     */
    public String getYoutubeRemoteCipher()
    {
        if(ytRemoteCipher.isEmpty() || ytRemoteCipher.equalsIgnoreCase("default"))
            return DEFAULT_REMOTE_CIPHER;
        if(ytRemoteCipher.equalsIgnoreCase("none") || ytRemoteCipher.equalsIgnoreCase("off"))
            return null;
        return ytRemoteCipher;
    }
    public String getYoutubeRemoteCipherPassword() { return ytRemoteCipherPassword.isEmpty() ? null : ytRemoteCipherPassword; }
    public Config getTransforms() { return transforms; }

    /**
     * Guarda en config.txt el refresh token de YouTube obtenido con OAuth,
     * para no tener que vincular la cuenta cada vez que se inicia el bot.
     */
    public synchronized boolean saveYoutubeRefreshToken(String refreshToken)
    {
        if(refreshToken == null || refreshToken.isBlank() || path == null)
            return false;
        try
        {
            String content = Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : "";
            String line = "youtube.refreshtoken = \"" + refreshToken.replace("\"", "") + "\"";
            if(content.matches("(?s).*refreshtoken\\s*=\\s*\"\".*"))
                content = content.replaceFirst("refreshtoken\\s*=\\s*\"\"", java.util.regex.Matcher.quoteReplacement("refreshtoken = \"" + refreshToken.replace("\"", "") + "\""));
            else
                content = content.trim() + "\n\n// guardado automáticamente al vincular YouTube\n" + line + "\n";
            Files.writeString(path, content, StandardCharsets.UTF_8);
            ytRefreshToken = refreshToken;
            return true;
        }
        catch(IOException e)
        {
            return false;
        }
    }

    public boolean isTooLong(AudioTrack track)
    {
        if(maxSeconds<=0)
            return false;
        return Math.round(track.getDuration()/1000.0) > maxSeconds;
    }
}
