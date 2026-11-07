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
package com.itstuki.tukibot.ytdlp;

import com.itstuki.tukibot.BotConfig;
import com.itstuki.tukibot.utils.OtherUtil;
import java.io.*;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Usa yt-dlp (https://github.com/yt-dlp/yt-dlp) para obtener el audio de
 * YouTube. yt-dlp se actualiza casi todas las semanas para seguir los cambios
 * de YouTube, así que es mucho más confiable que el descifrado de lavaplayer.
 *
 * Si no está instalado, el bot descarga solo los ejecutables oficiales de
 * yt-dlp y de Deno (el runtime de JavaScript que yt-dlp necesita para
 * YouTube) en la carpeta "bin" y mantiene yt-dlp actualizado.
 */
public class YtDlp
{
    private static final Logger LOG = LoggerFactory.getLogger("yt-dlp");
    private static final String FORMAT = "bestaudio[acodec=opus]/bestaudio[ext=webm]/bestaudio[ext=m4a]/bestaudio/best";

    private final BotConfig config;
    private final Path binDir = OtherUtil.getPath("bin");
    private final Path cacheDir = OtherUtil.getPath("bin").resolve("cache");
    private final boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    private final boolean mac = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    private final boolean arm = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT).matches(".*(aarch64|arm64).*");
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .proxy(ProxySelector.getDefault())
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private volatile String ytdlp;   // ruta o comando de yt-dlp
    private volatile String deno;    // ruta de deno (puede ser null si está en el PATH)
    private volatile boolean managed; // true si lo descargó el bot (se puede autoactualizar)

    public record Stream(String url, String ext, String userAgent) {}

    public YtDlp(BotConfig config)
    {
        this.config = config;
    }

    public boolean isEnabled()
    {
        return config.useYtDlp();
    }

    public boolean isReady()
    {
        return isEnabled() && ytdlp != null;
    }

    /** Busca o descarga los ejecutables en segundo plano. */
    public void init(ScheduledExecutorService scheduler)
    {
        if(!isEnabled())
            return;
        scheduler.submit(() -> {
            try
            {
                setup();
                if(managed)
                    scheduler.scheduleWithFixedDelay(this::selfUpdate, 0, 24, TimeUnit.HOURS);
            }
            catch(Exception e)
            {
                LOG.error("No se pudo preparar yt-dlp: " + e + ". YouTube usará solo el método interno (puede fallar).");
            }
        });
    }

    private void setup() throws IOException, InterruptedException
    {
        Files.createDirectories(binDir);
        Files.createDirectories(cacheDir);
        // restos de una ejecución anterior
        try(var files = Files.list(cacheDir))
        {
            files.forEach(f -> { try { Files.deleteIfExists(f); } catch(IOException ignored) {} });
        }

        // yt-dlp: ruta del config, carpeta bin, o PATH del sistema
        String custom = config.getYtDlpPath();
        Path local = binDir.resolve(windows ? "yt-dlp.exe" : "yt-dlp");
        if(custom != null && works(custom, "--version"))
            ytdlp = custom;
        else if(Files.exists(local) && works(local.toString(), "--version"))
        {
            ytdlp = local.toString();
            managed = true;
        }
        else if(works("yt-dlp", "--version"))
            ytdlp = "yt-dlp";
        else
        {
            LOG.info("Descargando yt-dlp (solo la primera vez)...");
            download("https://github.com/yt-dlp/yt-dlp/releases/latest/download/" + ytdlpAsset(), local);
            makeExecutable(local);
            ytdlp = local.toString();
            managed = true;
        }

        // Deno: carpeta bin o PATH del sistema
        Path localDeno = binDir.resolve(windows ? "deno.exe" : "deno");
        if(Files.exists(localDeno) && works(localDeno.toString(), "--version"))
            deno = localDeno.toString();
        else if(works("deno", "--version"))
            deno = null; // yt-dlp lo encuentra en el PATH
        else
        {
            LOG.info("Descargando Deno, el runtime de JavaScript que yt-dlp necesita para YouTube (solo la primera vez, ~40 MB)...");
            Path zip = binDir.resolve("deno.zip");
            download("https://github.com/denoland/deno/releases/latest/download/" + denoAsset(), zip);
            unzipSingle(zip, localDeno);
            Files.deleteIfExists(zip);
            makeExecutable(localDeno);
            deno = localDeno.toString();
        }
        LOG.info("yt-dlp listo (" + version() + "). YouTube se reproducirá con yt-dlp.");
        if(config.getYoutubeCookies() != null)
            LOG.info("yt-dlp usará las cookies de " + config.getYoutubeCookies());
        else if(config.getYoutubeCookiesBrowser() != null)
            LOG.info("yt-dlp usará las cookies del navegador " + config.getYoutubeCookiesBrowser());
    }

    private String ytdlpAsset()
    {
        if(windows)
            return "yt-dlp.exe";
        if(mac)
            return "yt-dlp_macos";
        return arm ? "yt-dlp_linux_aarch64" : "yt-dlp_linux";
    }

    private String denoAsset()
    {
        String arch = arm ? "aarch64" : "x86_64";
        if(windows)
            return "deno-x86_64-pc-windows-msvc.zip";
        if(mac)
            return "deno-" + arch + "-apple-darwin.zip";
        return "deno-" + arch + "-unknown-linux-gnu.zip";
    }

    private void download(String url, Path target) throws IOException, InterruptedException
    {
        Path tmp = target.resolveSibling(target.getFileName() + ".part");
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "TukiBot").timeout(Duration.ofMinutes(10)).GET().build();
        HttpResponse<Path> res = http.send(req, HttpResponse.BodyHandlers.ofFile(tmp));
        if(res.statusCode() != 200)
        {
            Files.deleteIfExists(tmp);
            throw new IOException("HTTP " + res.statusCode() + " al descargar " + url);
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void unzipSingle(Path zip, Path target) throws IOException
    {
        String wanted = target.getFileName().toString();
        try(ZipInputStream in = new ZipInputStream(Files.newInputStream(zip)))
        {
            ZipEntry e;
            while((e = in.getNextEntry()) != null)
                if(Paths.get(e.getName()).getFileName().toString().equals(wanted))
                {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
        }
        throw new IOException(wanted + " no está dentro de " + zip.getFileName());
    }

    private void makeExecutable(Path p)
    {
        if(!windows)
            p.toFile().setExecutable(true);
    }

    private boolean works(String cmd, String arg)
    {
        try
        {
            return run(List.of(cmd, arg), 30).exit == 0;
        }
        catch(Exception e)
        {
            return false;
        }
    }

    private String version()
    {
        try
        {
            return run(List.of(ytdlp, "--version"), 30).out.trim();
        }
        catch(Exception e)
        {
            return "?";
        }
    }

    private void selfUpdate()
    {
        try
        {
            Result r = run(List.of(ytdlp, "-U"), 180);
            String last = r.out.trim().isEmpty() ? "" : r.out.trim().lines().reduce((a, b) -> b).orElse("");
            if(last.toLowerCase(Locale.ROOT).contains("updated"))
                LOG.info("yt-dlp actualizado: " + last);
        }
        catch(Exception e)
        {
            LOG.debug("No se pudo actualizar yt-dlp: " + e);
        }
    }

    /** Obtiene la URL directa del audio de un video. Bloquea (usar fuera del hilo principal). */
    public Stream resolve(String videoUrl) throws IOException, InterruptedException
    {
        if(!isReady())
            throw new IOException("yt-dlp no está disponible");
        List<String> cmd = new ArrayList<>(List.of(ytdlp,
                "--no-playlist", "--no-warnings", "--no-progress", "--ignore-config",
                "-f", FORMAT,
                "--print", "%(ext)s|%(http_headers.User-Agent)s|%(url)s"));
        if(deno != null)
            cmd.addAll(List.of("--js-runtimes", "deno:" + deno));
        if(config.getYoutubeCookies() != null)
            cmd.addAll(List.of("--cookies", config.getYoutubeCookies()));
        else if(config.getYoutubeCookiesBrowser() != null)
            cmd.addAll(List.of("--cookies-from-browser", config.getYoutubeCookiesBrowser()));
        cmd.add(videoUrl);

        Result r = run(cmd, 60);
        String line = r.out.lines().filter(l -> l.contains("|http")).reduce((a, b) -> b).orElse(null);
        if(r.exit != 0 || line == null)
        {
            String err = r.err.lines().filter(l -> l.startsWith("ERROR")).findFirst().orElse(r.err.trim());
            throw new IOException(err.isEmpty() ? "yt-dlp terminó con código " + r.exit : err);
        }
        String[] parts = line.split("\\|", 3);
        String ua = parts[1].equals("NA") ? null : parts[1];
        return new Stream(parts[2].trim(), parts[0].trim(), ua);
    }

    /**
     * Hace que yt-dlp descargue el audio a un archivo temporal y lo devuelve
     * como stream que se puede leer mientras baja. yt-dlp se encarga de todos
     * los encabezados y tokens que YouTube exige.
     */
    public YtDlpDownload download(String videoUrl) throws IOException, InterruptedException
    {
        if(!isReady())
            throw new IOException("yt-dlp no está disponible");
        Files.createDirectories(cacheDir);
        Path file = cacheDir.resolve(UUID.randomUUID() + ".audio");
        List<String> cmd = new ArrayList<>(List.of(ytdlp,
                "--no-playlist", "--no-warnings", "--no-progress", "--ignore-config",
                "--no-part", "--no-mtime", "--no-cache-dir", "--force-overwrites",
                "-f", FORMAT,
                "--print", "before_dl:%(ext)s",
                "-o", file.toAbsolutePath().toString()));
        if(deno != null)
            cmd.addAll(List.of("--js-runtimes", "deno:" + deno));
        if(config.getYoutubeCookies() != null)
            cmd.addAll(List.of("--cookies", config.getYoutubeCookies()));
        else if(config.getYoutubeCookiesBrowser() != null)
            cmd.addAll(List.of("--cookies-from-browser", config.getYoutubeCookiesBrowser()));
        cmd.add(videoUrl);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        if(deno != null)
        {
            String path = System.getenv().getOrDefault(windows ? "Path" : "PATH", "");
            pb.environment().put(windows ? "Path" : "PATH", binDir.toAbsolutePath() + File.pathSeparator + path);
        }
        Process p = pb.start();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Thread.ofVirtual().start(() -> copy(p.getErrorStream(), err));

        // la primera línea es la extensión, que se imprime justo antes de descargar
        BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
        java.util.concurrent.CompletableFuture<String> extFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try
            {
                String line = out.readLine();
                Thread.ofVirtual().start(() -> copy(p.getInputStream(), OutputStream.nullOutputStream()));
                return line == null ? null : line.trim();
            }
            catch(IOException e)
            {
                return null;
            }
        }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        String ext;
        try
        {
            ext = extFuture.get(60, TimeUnit.SECONDS);
        }
        catch(java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e)
        {
            ext = null;
        }
        if(ext == null || ext.isEmpty())
        {
            p.destroyForcibly();
            p.waitFor(5, TimeUnit.SECONDS);
            String msg = err.toString(StandardCharsets.UTF_8).lines().filter(l -> l.startsWith("ERROR")).findFirst()
                    .orElse("yt-dlp no pudo obtener el audio");
            Files.deleteIfExists(file);
            throw new IOException(msg);
        }
        return new YtDlpDownload(p, file, ext, err);
    }

    private record Result(int exit, String out, String err) {}

    private Result run(List<String> cmd, int timeoutSeconds) throws IOException, InterruptedException
    {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if(deno != null)
        {
            // por si acaso, Deno también queda en el PATH del proceso
            String path = System.getenv().getOrDefault(windows ? "Path" : "PATH", "");
            pb.environment().put(windows ? "Path" : "PATH", binDir.toAbsolutePath() + File.pathSeparator + path);
        }
        Process p = pb.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Thread t1 = Thread.ofVirtual().start(() -> copy(p.getInputStream(), out));
        Thread t2 = Thread.ofVirtual().start(() -> copy(p.getErrorStream(), err));
        if(!p.waitFor(timeoutSeconds, TimeUnit.SECONDS))
        {
            p.destroyForcibly();
            throw new IOException("yt-dlp tardó más de " + timeoutSeconds + " s");
        }
        t1.join(2000);
        t2.join(2000);
        return new Result(p.exitValue(), out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    private static void copy(InputStream in, OutputStream out)
    {
        try(in)
        {
            in.transferTo(out);
        }
        catch(IOException ignored) {}
    }
}
