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
package com.itstuki.tukibot.audio;

import com.github.natanbc.lavadsp.karaoke.KaraokePcmAudioFilter;
import com.github.natanbc.lavadsp.lowpass.LowPassPcmAudioFilter;
import com.github.natanbc.lavadsp.rotation.RotationPcmAudioFilter;
import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.github.natanbc.lavadsp.vibrato.VibratoPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer;
import java.util.List;

/**
 * Filtros de audio predefinidos para /filter. Usan los filtros de lavaplayer
 * y lavadsp; el audio se procesa en el bot, así que gastan algo más de CPU.
 */
public enum AudioFilter
{
    OFF("Sin filtro", "⚪"),
    BASSBOOST("Bass boost", "🔊"),
    NIGHTCORE("Nightcore", "🌙"),
    VAPORWAVE("Vaporwave", "🌴"),
    EIGHT_D("8D", "🎧"),
    KARAOKE("Karaoke", "🎤"),
    SOFT("Suave", "☁️"),
    TREBLE("Agudos", "✨"),
    VIBRATO("Vibrato", "〰️");

    private final String label;
    private final String emoji;

    AudioFilter(String label, String emoji)
    {
        this.label = label;
        this.emoji = emoji;
    }

    public String getLabel() { return label; }
    public String getEmoji() { return emoji; }

    /** Fábrica de filtros para lavaplayer, o null si no hay filtro. */
    public PcmFilterFactory factory()
    {
        if(this == OFF)
            return null;
        return (track, format, output) -> {
            int channels = format.channelCount;
            int rate = format.sampleRate;
            FloatPcmAudioFilter filter;
            try
            {
                filter = build(channels, rate, output);
            }
            catch(Throwable t)
            {
                // por ejemplo, si la librería nativa de timescale no existe para este sistema
                org.slf4j.LoggerFactory.getLogger("Filtros").warn("No pude activar el filtro " + label + ": " + t);
                filter = null;
            }
            return filter == null ? List.of() : List.of(filter);
        };
    }

    private FloatPcmAudioFilter build(int channels, int rate, FloatPcmAudioFilter output)
    {
        {
            return switch(this)
            {
                case BASSBOOST -> equalizer(channels, output, new float[]{0.25f, 0.2f, 0.15f, 0.1f, 0.05f, 0f, -0.05f});
                case TREBLE -> equalizer(channels, output, new float[]{-0.05f, -0.05f, 0f, 0f, 0f, 0f, 0f, 0.05f, 0.1f, 0.15f, 0.2f, 0.2f, 0.2f, 0.2f, 0.2f});
                case NIGHTCORE -> new TimescalePcmAudioFilter(output, channels, rate).setRate(1.25);
                case VAPORWAVE -> new TimescalePcmAudioFilter(output, channels, rate).setRate(0.8);
                case EIGHT_D -> new RotationPcmAudioFilter(output, rate).setRotationSpeed(0.2);
                case KARAOKE -> new KaraokePcmAudioFilter(output, channels, rate)
                        .setLevel(1f).setMonoLevel(1f).setFilterBand(220f).setFilterWidth(100f);
                case SOFT -> new LowPassPcmAudioFilter(output, channels).setSmoothing(20f);
                case VIBRATO -> new VibratoPcmAudioFilter(output, channels, rate).setFrequency(4f).setDepth(0.75f);
                default -> null;
            };
        }
    }

    private static Equalizer equalizer(int channels, FloatPcmAudioFilter output, float[] gains)
    {
        Equalizer eq = new Equalizer(channels, output);
        for(int i = 0; i < gains.length && i < Equalizer.BAND_COUNT; i++)
            eq.setGain(i, gains[i]);
        return eq;
    }
}
