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
package com.itstuki.tukibot.lyrics;

import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Busca letras en LRCLIB (https://lrclib.net), una API abierta y gratuita.
 * Reemplaza a JLyrics, que dependía de hacer scraping de sitios que ya
 * cambiaron su HTML.
 */
public class LyricsClient
{
    private static final Logger LOG = LoggerFactory.getLogger("Letras");
    private static final String USER_AGENT = "TukiBot/1.0 (https://github.com/ItsTuki/Bot-Discord)";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .proxy(ProxySelector.getDefault())
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public record Lyrics(String title, String artist, String content, String url) {}

    /**
     * @param title  título de la canción (puede venir "sucio" desde YouTube)
     * @param artist artista o canal, puede ser null
     */
    public CompletableFuture<Lyrics> getLyrics(String title, String artist)
    {
        String query = cleanTitle(title);
        if(artist != null && !artist.isBlank() && !query.toLowerCase().contains(cleanArtist(artist).toLowerCase()))
            query = cleanArtist(artist) + " " + query;
        String url = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(15))
                .GET().build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    if(res.statusCode() != 200)
                        return null;
                    JSONArray arr = new JSONArray(res.body());
                    for(int i = 0; i < arr.length(); i++)
                    {
                        JSONObject o = arr.getJSONObject(i);
                        String plain = o.optString("plainLyrics", "");
                        if(o.optBoolean("instrumental", false) || plain.isBlank())
                            continue;
                        String name = o.optString("trackName", title);
                        String by = o.optString("artistName", "");
                        return new Lyrics(name, by, plain.trim(),
                                "https://lrclib.net/search/" + URLEncoder.encode(by + " " + name, StandardCharsets.UTF_8).replace("+", "%20"));
                    }
                    return null;
                })
                .exceptionally(t -> {
                    LOG.warn("Error buscando letra para '" + title + "': " + t);
                    return null;
                });
    }

    /** Quita los adornos típicos de los títulos de YouTube. */
    static String cleanTitle(String title)
    {
        if(title == null)
            return "";
        return title
                .replaceAll("(?i)[\\(\\[][^\\)\\]]*(official|oficial|video|audio|lyric|letra|visualizer|hd|4k|mv|live|en vivo)[^\\)\\]]*[\\)\\]]", "")
                .replaceAll("(?i)\\b(ft\\.?|feat\\.?)\\s.*$", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    static String cleanArtist(String artist)
    {
        return artist.replaceAll("(?i)\\s*-\\s*topic$", "").replaceAll("(?i)vevo$", "").trim();
    }
}
