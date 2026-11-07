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
package com.itstuki.tukibot.update;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.TukiBot;
import com.itstuki.tukibot.utils.OtherUtil;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actualización automática desde los releases de GitHub. Descarga el jar
 * nuevo al lado del actual como {@code <jar>.new} y lo pone en su lugar al
 * apagar el bot (así nunca se reemplaza un jar que se está usando). En
 * Windows, donde el jar queda bloqueado, lo aplica iniciar.bat.
 */
public class Updater
{
    private static final Logger LOG = LoggerFactory.getLogger("Actualizador");
    public static final String REPO = "ItsTuki/Bot-Discord";
    private static final String ASSET = "TukiBot.jar";

    private final Bot bot;
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15)).build();
    private volatile String downloadedVersion = null;

    public Updater(Bot bot)
    {
        this.bot = bot;
    }

    /** Jar en ejecución, o null si se corre desde el código fuente. */
    public static Path currentJar()
    {
        try
        {
            Path p = Path.of(TukiBot.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Files.isRegularFile(p) && p.toString().endsWith(".jar") ? p : null;
        }
        catch(Exception e)
        {
            return null;
        }
    }

    private static Path pendingFile(Path jar)
    {
        return jar.resolveSibling(jar.getFileName() + ".new");
    }

    public void start()
    {
        if(!bot.getConfig().useAutoUpdate())
        {
            LOG.info("Actualización automática desactivada.");
            return;
        }
        if(currentJar() == null || "DEV".equals(OtherUtil.getCurrentVersion()))
        {
            LOG.info("Actualización automática: no se está ejecutando desde un jar de release, se omite.");
            return;
        }
        bot.getThreadpool().scheduleWithFixedDelay(this::check, 2, 6 * 60, TimeUnit.MINUTES);
    }

    /** Compara versiones tipo 1.2.10 (ignora la "v" y sufijos como -beta). */
    static int compare(String a, String b)
    {
        String[] x = a.replaceFirst("^[vV]", "").split("[.-]");
        String[] y = b.replaceFirst("^[vV]", "").split("[.-]");
        for(int i = 0; i < Math.max(x.length, y.length); i++)
        {
            int xi = i < x.length ? parse(x[i]) : 0;
            int yi = i < y.length ? parse(y[i]) : 0;
            if(xi != yi)
                return Integer.compare(xi, yi);
        }
        return 0;
    }

    private static int parse(String s)
    {
        try { return Integer.parseInt(s.replaceAll("\\D.*", "")); }
        catch(NumberFormatException e) { return 0; }
    }

    private void check()
    {
        try
        {
            HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + REPO + "/releases/latest"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "TukiBot-Updater")
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            if(res.statusCode() != 200)
            {
                LOG.debug("GitHub respondió HTTP " + res.statusCode() + " al buscar actualizaciones.");
                return;
            }
            JSONObject release = new JSONObject(res.body());
            String latest = release.optString("tag_name", "");
            String current = OtherUtil.getCurrentVersion();
            if(latest.isEmpty() || compare(latest, current) <= 0 || latest.equals(downloadedVersion))
                return;

            JSONArray assets = release.optJSONArray("assets");
            String url = null;
            long size = -1;
            for(int i = 0; assets != null && i < assets.length(); i++)
            {
                JSONObject a = assets.getJSONObject(i);
                if(ASSET.equals(a.optString("name")))
                {
                    url = a.optString("browser_download_url");
                    size = a.optLong("size", -1);
                }
            }
            if(url == null)
                return;

            Path jar = currentJar();
            Path pending = pendingFile(jar);
            Path tmp = jar.resolveSibling(jar.getFileName() + ".download");
            LOG.info("Hay una versión nueva (" + latest + "); descargándola...");
            HttpResponse<InputStream> dl = http.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(5)).header("User-Agent", "TukiBot-Updater").GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            if(dl.statusCode() != 200)
                throw new IllegalStateException("HTTP " + dl.statusCode());
            try(InputStream in = dl.body())
            {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            byte[] head = new byte[2];
            try(InputStream in = Files.newInputStream(tmp)) { in.read(head); }
            if((size > 0 && Files.size(tmp) != size) || head[0] != 'P' || head[1] != 'K')
            {
                Files.deleteIfExists(tmp);
                throw new IllegalStateException("el archivo descargado está incompleto");
            }
            Files.move(tmp, pending, StandardCopyOption.REPLACE_EXISTING);
            downloadedVersion = latest;
            LOG.info("TukiBot " + latest + " descargado; se aplicará al reiniciar.");
            String notes = release.optString("html_url", "https://github.com/" + REPO + "/releases/latest");
            bot.getAlerts().send("update-" + latest, "⬆️ **TukiBot " + latest + "** ya está descargado (tienes " + current + ").\n"
                    + "Se instala solo la próxima vez que reinicies el bot. Novedades: " + notes, Long.MAX_VALUE);
        }
        catch(Exception e)
        {
            LOG.warn("No pude buscar actualizaciones: " + e.getMessage());
        }
    }

    /**
     * Pone el jar descargado en lugar del actual. Se llama al apagar, cuando
     * ya no se cargan más clases. En Windows puede fallar porque el jar está
     * en uso; en ese caso queda el .new y lo aplica iniciar.bat.
     */
    public static void applyPending()
    {
        Path jar = currentJar();
        if(jar == null)
            return;
        Path pending = pendingFile(jar);
        if(!Files.exists(pending))
            return;
        try
        {
            Files.move(pending, jar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            System.out.println("[TukiBot] Actualización instalada; la nueva versión se usará al iniciar.");
        }
        catch(Exception e)
        {
            System.out.println("[TukiBot] La actualización quedó en " + pending.getFileName() + " (se aplica con iniciar.bat / iniciar.sh).");
        }
    }
}
