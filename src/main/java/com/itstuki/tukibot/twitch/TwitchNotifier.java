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
package com.itstuki.tukibot.twitch;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.settings.Settings;
import com.itstuki.tukibot.ui.Panels;
import com.itstuki.tukibot.utils.FormatUtil;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Revisa cada cierto tiempo si el canal de Twitch configurado está en
 * directo (API Helix oficial) y lo anuncia en los servidores que eligieron un
 * canal con /config twitch.
 */
public class TwitchNotifier
{
    private static final Logger LOG = LoggerFactory.getLogger("Twitch");

    public record Stream(String id, String userName, String title, String game, int viewers, Instant startedAt, String thumbnail) {}

    private final Bot bot;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private String accessToken = null;
    private Instant tokenExpires = Instant.EPOCH;
    private volatile Stream current = null;
    private volatile String lastAnnouncedId = null;
    private volatile boolean firstCheck = true;
    private volatile boolean working = false;

    public TwitchNotifier(Bot bot)
    {
        this.bot = bot;
    }

    public boolean isEnabled()
    {
        return bot.getConfig().useTwitchNotify();
    }

    /** true si ya se pudo consultar Twitch al menos una vez. */
    public boolean isWorking()
    {
        return working;
    }

    /** El directo actual, o null si está offline (o si no se sabe). */
    public Stream getCurrent()
    {
        return current;
    }

    public void start()
    {
        if(!isEnabled())
        {
            LOG.info("Avisos de Twitch desactivados (falta twitchnotify.clientid / clientsecret en config.txt).");
            return;
        }
        int interval = bot.getConfig().getTwitchInterval();
        bot.getThreadpool().scheduleWithFixedDelay(this::check, 5, interval, TimeUnit.SECONDS);
        LOG.info("Avisos de Twitch activados para " + bot.getConfig().getTwitch() + " (cada " + interval + " s).");
    }

    private synchronized String token() throws Exception
    {
        if(accessToken != null && Instant.now().isBefore(tokenExpires.minusSeconds(300)))
            return accessToken;
        String body = "client_id=" + enc(bot.getConfig().getTwitchClientId())
                + "&client_secret=" + enc(bot.getConfig().getTwitchClientSecret())
                + "&grant_type=client_credentials";
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create("https://id.twitch.tv/oauth2/token"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        if(res.statusCode() != 200)
            throw new IllegalStateException("Twitch rechazó el Client ID / Secret (HTTP " + res.statusCode() + "): " + FormatUtil.truncate(res.body(), 200));
        JSONObject json = new JSONObject(res.body());
        accessToken = json.getString("access_token");
        tokenExpires = Instant.now().plusSeconds(json.optLong("expires_in", 3600));
        return accessToken;
    }

    private static String enc(String s)
    {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Consulta Helix. Devuelve el directo o null si está offline. */
    public Stream fetch() throws Exception
    {
        for(int attempt = 0; attempt < 2; attempt++)
        {
            HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(
                            "https://api.twitch.tv/helix/streams?user_login=" + enc(bot.getConfig().getTwitch().toLowerCase())))
                    .timeout(Duration.ofSeconds(15))
                    .header("Client-Id", bot.getConfig().getTwitchClientId())
                    .header("Authorization", "Bearer " + token())
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            if(res.statusCode() == 401 && attempt == 0)
            {
                synchronized(this) { accessToken = null; }
                continue;
            }
            if(res.statusCode() != 200)
                throw new IllegalStateException("HTTP " + res.statusCode() + ": " + FormatUtil.truncate(res.body(), 200));
            JSONArray data = new JSONObject(res.body()).optJSONArray("data");
            if(data == null || data.isEmpty())
                return null;
            JSONObject s = data.getJSONObject(0);
            Instant started;
            try { started = Instant.parse(s.optString("started_at")); } catch(Exception e) { started = Instant.now(); }
            return new Stream(s.optString("id"), s.optString("user_name", bot.getConfig().getTwitch()), s.optString("title"),
                    s.optString("game_name"), s.optInt("viewer_count"), started,
                    s.optString("thumbnail_url").replace("{width}", "1280").replace("{height}", "720"));
        }
        return null;
    }

    private void check()
    {
        try
        {
            Stream stream = fetch();
            working = true;
            current = stream;
            if(stream == null)
            {
                lastAnnouncedId = null;
                firstCheck = false;
                return;
            }
            if(stream.id().equals(lastAnnouncedId))
                return;
            lastAnnouncedId = stream.id();
            // si el bot arranca con el directo ya empezado hace rato, no se avisa de nuevo
            boolean freshStart = Duration.between(stream.startedAt(), Instant.now()).toMinutes() < 10;
            if(!firstCheck || freshStart)
                announce(stream);
            firstCheck = false;
        }
        catch(Exception e)
        {
            LOG.warn("No pude revisar Twitch: " + e.getMessage());
        }
    }

    private void announce(Stream stream)
    {
        if(bot.getJDA() == null)
            return;
        int sent = 0;
        for(Guild guild : bot.getJDA().getGuilds())
        {
            Settings s = bot.getSettingsManager().getSettings(guild);
            if(s.getTwitchChannelId() == 0L)
                continue;
            GuildMessageChannel channel = guild.getChannelById(GuildMessageChannel.class, s.getTwitchChannelId());
            if(channel == null || !guild.getSelfMember().hasPermission(channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND))
                continue;
            Role role = s.getTwitchRoleId() == 0L ? null : guild.getRoleById(s.getTwitchRoleId());
            var action = channel.sendMessage(message(guild, stream, role));
            if(role != null)
                action = action.mentionRoles(role.getId());
            action.queue(m -> {}, f -> LOG.warn("No pude anunciar el directo en " + guild.getName() + ": " + f.getMessage()));
            sent++;
        }
        LOG.info("Directo de " + stream.userName() + " anunciado en " + sent + " servidores.");
    }

    public MessageCreateData message(Guild guild, Stream stream, Role role)
    {
        String url = "https://www.twitch.tv/" + bot.getConfig().getTwitch();
        List<ContainerChildComponent> children = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        text.append("-# 🔴 EN VIVO");
        if(role != null)
            text.append(" · ").append(role.getAsMention());
        text.append("\n## ").append(FormatUtil.escape(stream.userName())).append(" está en directo en Twitch\n");
        text.append("### [").append(FormatUtil.escape(FormatUtil.truncate(stream.title().isBlank() ? "Sin título" : stream.title(), 200)))
                .append("](").append(url).append(")\n");
        List<String> meta = new ArrayList<>();
        if(!stream.game().isBlank())
            meta.add("🎮 " + FormatUtil.escape(stream.game()));
        meta.add("👁️ " + stream.viewers() + " espectadores");
        meta.add("⏱️ desde <t:" + stream.startedAt().getEpochSecond() + ":R>");
        text.append(String.join(" · ", meta));
        children.add(TextDisplay.of(text.toString()));
        if(stream.thumbnail() != null && stream.thumbnail().startsWith("http"))
            // el parámetro evita que Discord muestre una miniatura vieja guardada en caché
            children.add(MediaGallery.of(MediaGalleryItem.fromUrl(stream.thumbnail() + "?t=" + Instant.now().getEpochSecond())));
        children.add(Separator.createDivider(Separator.Spacing.SMALL));
        children.add(ActionRow.of(Button.link(url, "Ver en Twitch").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("🟣"))));
        return Panels.container(bot, guild, children, new java.awt.Color(0x9146FF));
    }
}
