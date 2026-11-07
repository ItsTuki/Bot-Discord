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

import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Audio que yt-dlp está descargando a un archivo temporal. Se puede leer
 * mientras baja (yt-dlp descarga mucho más rápido que el tiempo real) y
 * permite adelantar dentro de lo ya descargado. Al cerrarse, detiene yt-dlp
 * y borra el archivo.
 */
public class YtDlpDownload extends SeekableInputStream
{
    private final Process process;
    private final Path file;
    private final String ext;
    private final ByteArrayOutputStream stderr;
    private RandomAccessFile raf;
    private long position = 0;
    private volatile boolean closed = false;

    YtDlpDownload(Process process, Path file, String ext, ByteArrayOutputStream stderr)
    {
        super(Units.CONTENT_LENGTH_UNKNOWN, 0);
        this.process = process;
        this.file = file;
        this.ext = ext;
        this.stderr = stderr;
    }

    public String getExt()
    {
        return ext;
    }

    /** Espera a que existan los primeros bytes (o a que yt-dlp termine con error). */
    public void awaitStart(long timeoutMillis) throws IOException, InterruptedException
    {
        long end = System.currentTimeMillis() + timeoutMillis;
        while(System.currentTimeMillis() < end)
        {
            if(Files.exists(file) && Files.size(file) > 0)
                return;
            if(!process.isAlive())
            {
                if(Files.exists(file) && Files.size(file) > 0)
                    return;
                throw new IOException(error());
            }
            Thread.sleep(50);
        }
        throw new IOException("yt-dlp no empezó a descargar a tiempo");
    }

    private String error()
    {
        String err = stderr.toString(StandardCharsets.UTF_8);
        return err.lines().filter(l -> l.startsWith("ERROR")).findFirst()
                .orElse(err.isBlank() ? "yt-dlp terminó con código " + process.exitValue() : err.trim());
    }

    private RandomAccessFile raf() throws IOException
    {
        if(raf == null)
            raf = new RandomAccessFile(file.toFile(), "r");
        return raf;
    }

    /** Espera hasta que haya datos en la posición actual. Devuelve false si se llegó al final. */
    private boolean waitForData(long needed) throws IOException
    {
        while(!closed)
        {
            long size = Files.exists(file) ? Files.size(file) : 0;
            if(size >= needed)
                return true;
            if(!process.isAlive())
            {
                size = Files.exists(file) ? Files.size(file) : 0;
                if(size >= needed)
                    return true;
                if(process.exitValue() != 0 && size == 0)
                    throw new IOException(error());
                return false;
            }
            try
            {
                Thread.sleep(20);
            }
            catch(InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new IOException("Lectura interrumpida", e);
            }
        }
        return false;
    }

    @Override
    public int read() throws IOException
    {
        if(!waitForData(position + 1))
            return -1;
        RandomAccessFile r = raf();
        r.seek(position);
        int b = r.read();
        if(b >= 0)
            position++;
        return b;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException
    {
        if(length == 0)
            return 0;
        if(!waitForData(position + 1))
            return -1;
        RandomAccessFile r = raf();
        r.seek(position);
        int available = (int) Math.min(length, Math.max(1, Files.size(file) - position));
        int n = r.read(buffer, offset, available);
        if(n > 0)
            position += n;
        return n;
    }

    @Override
    public long getPosition()
    {
        return position;
    }

    @Override
    protected void seekHard(long target) throws IOException
    {
        waitForData(target);
        position = target;
    }

    @Override
    public boolean canSeekHard()
    {
        return true;
    }

    @Override
    public List<AudioTrackInfoProvider> getTrackInfoProviders()
    {
        return Collections.emptyList();
    }

    @Override
    public void close() throws IOException
    {
        closed = true;
        if(process.isAlive())
            process.destroyForcibly();
        try
        {
            if(raf != null)
                raf.close();
        }
        finally
        {
            try
            {
                process.waitFor(java.util.concurrent.TimeUnit.SECONDS.toMillis(5), java.util.concurrent.TimeUnit.MILLISECONDS);
            }
            catch(InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            Files.deleteIfExists(file);
        }
    }
}
