package com.itstuki.tukibot.audio;

import static org.junit.jupiter.api.Assertions.*;

import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class AudioFilterTest
{
    /** Salida falsa que cuenta las muestras que le llegan. */
    static class Sink implements UniversalPcmAudioFilter
    {
        final AtomicLong samples = new AtomicLong();
        public void process(float[][] input, int offset, int length) { samples.addAndGet(length); }
        public void process(short[] input, int offset, int length) { samples.addAndGet(length); }
        public void process(ShortBuffer buffer) { samples.addAndGet(buffer.remaining()); }
        public void process(short[][] input, int offset, int length) { samples.addAndGet(length); }
        public void seekPerformed(long requestedTime, long providedTime) {}
        public void flush() {}
        public void close() {}
    }

    @Test
    void everyPresetProcessesAudio() throws Exception
    {
        var format = StandardAudioDataFormats.DISCORD_OPUS;
        for(com.itstuki.tukibot.audio.AudioFilter preset : com.itstuki.tukibot.audio.AudioFilter.values())
        {
            if(preset == com.itstuki.tukibot.audio.AudioFilter.OFF)
            {
                assertNull(preset.factory());
                continue;
            }
            Sink sink = new Sink();
            List<AudioFilter> chain = preset.factory().buildChain(null, format, sink);
            assertEquals(1, chain.size(), preset.name());
            FloatPcmAudioFilter f = (FloatPcmAudioFilter) chain.get(0);
            float[][] buf = new float[2][960];
            for(int i = 0; i < 960; i++)
                buf[0][i] = buf[1][i] = (float) Math.sin(i / 10.0) * 0.5f;
            for(int i = 0; i < 50; i++)
                f.process(buf, 0, 960);
            f.flush();
            assertTrue(sink.samples.get() > 0, preset.name() + " no entregó audio");
        }
    }
}
