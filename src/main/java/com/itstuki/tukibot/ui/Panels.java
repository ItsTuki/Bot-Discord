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
package com.itstuki.tukibot.ui;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.audio.AudioHandler;
import com.itstuki.tukibot.audio.QueuedTrack;
import com.itstuki.tukibot.audio.RequestMetadata;
import com.itstuki.tukibot.settings.QueueType;
import com.itstuki.tukibot.settings.RepeatMode;
import com.itstuki.tukibot.settings.Settings;
import com.itstuki.tukibot.utils.FormatUtil;
import com.itstuki.tukibot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

/**
 * Paneles con Components V2 (contenedores, secciones y botones) que
 * reemplazan los embeds y reacciones de JMusicBot.
 */
public final class Panels
{
    public static final int QUEUE_PAGE_SIZE = 10;

    private Panels() {}

    public static Color accent(Bot bot, Guild guild)
    {
        if(bot.getConfig().getColor() != null)
            return bot.getConfig().getColor();
        return guild == null ? null : guild.getSelfMember().getColor();
    }

    public static MessageCreateData container(Bot bot, Guild guild, List<? extends ContainerChildComponent> children)
    {
        return container(bot, guild, children, accent(bot, guild));
    }

    public static MessageCreateData container(Bot bot, Guild guild, List<? extends ContainerChildComponent> children, Color color)
    {
        Container container = Container.of(children).withAccentColor(color);
        return new MessageCreateBuilder().useComponentsV2().setComponents(container).build();
    }

    /** Mensaje simple dentro de un contenedor. */
    public static MessageCreateData simple(Bot bot, Guild guild, String text)
    {
        return container(bot, guild, List.of(TextDisplay.of(FormatUtil.truncate(text, 3900))));
    }

    // ------------------------------------------------------------------
    // Reproduciendo ahora
    // ------------------------------------------------------------------

    public static MessageCreateData nowPlaying(Bot bot, Guild guild, AudioHandler handler)
    {
        AudioTrack track = handler.getPlayer().getPlayingTrack();
        // no se exige estar conectado: al empezar una canción la conexión de voz puede tardar un poco
        if(track == null)
            return noMusic(bot, guild, handler);

        Settings settings = bot.getSettingsManager().getSettings(guild);
        RequestMetadata rm = handler.getRequestMetadata();
        GuildVoiceState vs = guild.getSelfMember().getVoiceState();

        List<ContainerChildComponent> children = new ArrayList<>();

        StringBuilder header = new StringBuilder();
        header.append("-# ").append(bot.getConfig().getSuccess()).append(" Reproduciendo ahora");
        if(vs != null && vs.getChannel() != null)
            header.append(" en ").append(vs.getChannel().getAsMention());
        header.append("\n### ").append(FormatUtil.trackLink(track));
        StringBuilder sub = new StringBuilder();
        if(track.getInfo().author != null && !track.getInfo().author.isBlank())
            sub.append("👤 ").append(FormatUtil.escape(track.getInfo().author)); // 👤
        if(rm.getOwner() != 0L)
            sub.append(sub.isEmpty() ? "" : " · ").append("pedida por <@").append(rm.getOwner()).append(">");
        if(!sub.isEmpty())
            header.append("\n").append(sub);

        String artwork = track.getInfo().artworkUrl;
        if(bot.getConfig().useNPImages() && artwork != null && artwork.startsWith("http"))
            children.add(Section.of(Thumbnail.fromUrl(artwork), TextDisplay.of(header.toString())));
        else
            children.add(TextDisplay.of(header.toString()));

        double progress = track.getInfo().isStream ? -1 : (double) track.getPosition() / Math.max(1, track.getDuration());
        String progressLine = handler.getStatusEmoji() + " " + FormatUtil.progressBar(progress)
                + " `[" + TimeUtil.formatTime(track.getPosition()) + " / " + TimeUtil.formatTime(track.getDuration()) + "]`"
                + "  " + FormatUtil.volumeIcon(handler.getPlayer().getVolume()) + " `" + handler.getPlayer().getVolume() + "%`";
        children.add(TextDisplay.of(progressLine));
        children.add(Separator.createDivider(Separator.Spacing.SMALL));

        StringBuilder footer = new StringBuilder("-# 📜 ").append(handler.getQueue().size()).append(" en cola"); // 📜
        if(!handler.getQueue().isEmpty())
            footer.append(" · Siguiente: ").append(FormatUtil.escape(FormatUtil.truncate(handler.getQueue().get(0).getTrack().getInfo().title, 60)));
        if(settings.getRepeatMode() != RepeatMode.OFF)
            footer.append(" · ").append(settings.getRepeatMode().getEmoji()).append(" ").append(settings.getRepeatMode().getUserFriendlyName());
        if(settings.isAutoplay())
            footer.append(" · 📻 Autoplay");
        if(handler.getFilter() != com.itstuki.tukibot.audio.AudioFilter.OFF)
            footer.append(" · ").append(handler.getFilter().getEmoji()).append(" ").append(handler.getFilter().getLabel());
        children.add(TextDisplay.of(footer.toString()));

        boolean paused = handler.getPlayer().isPaused();
        children.add(ActionRow.of(
                (paused ? Button.success("np:pause", "Reanudar") : Button.secondary("np:pause", "Pausar"))
                        .withEmoji(Emoji.fromUnicode(paused ? "▶️" : "⏸️")),
                Button.primary("np:skip", "Saltar").withEmoji(Emoji.fromUnicode("⏭️")),
                Button.danger("np:stop", "Detener").withEmoji(Emoji.fromUnicode("⏹️")),
                Button.secondary("np:repeat", Emoji.fromUnicode(settings.getRepeatMode() == RepeatMode.SINGLE ? "🔂" : "🔁"))
                        .withStyle(settings.getRepeatMode() == RepeatMode.OFF ? net.dv8tion.jda.api.components.buttons.ButtonStyle.SECONDARY
                                : net.dv8tion.jda.api.components.buttons.ButtonStyle.SUCCESS),
                Button.secondary("np:shuffle", Emoji.fromUnicode("🔀"))));
        children.add(ActionRow.of(
                Button.secondary("np:voldown", Emoji.fromUnicode("🔉")),
                Button.secondary("np:volup", Emoji.fromUnicode("🔊")),
                Button.secondary("np:queue", "Cola").withEmoji(Emoji.fromUnicode("📜")),
                Button.secondary("np:lyrics", "Letra").withEmoji(Emoji.fromUnicode("🎤")),
                (settings.isAutoplay() ? Button.success("np:autoplay", "Autoplay") : Button.secondary("np:autoplay", "Autoplay"))
                        .withEmoji(Emoji.fromUnicode("📻"))));

        return container(bot, guild, children);
    }

    public static MessageCreateData noMusic(Bot bot, Guild guild, AudioHandler handler)
    {
        int volume = handler == null ? 100 : handler.getPlayer().getVolume();
        return container(bot, guild, List.of(
                TextDisplay.of("### " + AudioHandler.STOP_EMOJI + " No hay música sonando\n"
                        + FormatUtil.progressBar(-1) + "  " + FormatUtil.volumeIcon(volume) + " `" + volume + "%`"),
                TextDisplay.of("-# Usa /play para agregar una canción.")));
    }

    // ------------------------------------------------------------------
    // Cola
    // ------------------------------------------------------------------

    public static int totalPages(AudioHandler handler)
    {
        return Math.max(1, (int) Math.ceil(handler.getQueue().size() / (double) QUEUE_PAGE_SIZE));
    }

    public static MessageCreateData queue(Bot bot, Guild guild, AudioHandler handler, int page)
    {
        List<QueuedTrack> list = handler.getQueue().getList();
        Settings settings = bot.getSettingsManager().getSettings(guild);
        int pages = totalPages(handler);
        page = Math.max(1, Math.min(page, pages));

        List<ContainerChildComponent> children = new ArrayList<>();
        StringBuilder header = new StringBuilder("### 📜 Cola de reproducción");
        AudioTrack current = handler.getPlayer().getPlayingTrack();
        if(current != null)
            header.append("\n").append(handler.getStatusEmoji()).append(" ").append(FormatUtil.trackLink(current))
                    .append(" `[").append(TimeUtil.formatTime(current.getPosition())).append(" / ")
                    .append(TimeUtil.formatTime(current.getDuration())).append("]`");
        children.add(TextDisplay.of(header.toString()));
        children.add(Separator.createDivider(Separator.Spacing.SMALL));

        long total = 0;
        for(QueuedTrack qt : list)
            total += qt.getTrack().getDuration();

        if(list.isEmpty())
            children.add(TextDisplay.of(bot.getConfig().getWarning() + " No hay canciones en la cola."));
        else
        {
            StringBuilder sb = new StringBuilder();
            int start = (page - 1) * QUEUE_PAGE_SIZE;
            for(int i = start; i < Math.min(list.size(), start + QUEUE_PAGE_SIZE); i++)
                sb.append("`").append(i + 1).append(".` ").append(list.get(i)).append("\n");
            children.add(TextDisplay.of(FormatUtil.truncate(sb.toString().trim(), 3000)));
        }

        children.add(Separator.createDivider(Separator.Spacing.SMALL));
        QueueType qtype = settings.getQueueType();
        RepeatMode repeat = settings.getRepeatMode();
        children.add(TextDisplay.of("-# " + list.size() + " canciones · `" + TimeUtil.formatTime(total) + "` en total · "
                + qtype.getEmoji() + " " + qtype.getUserFriendlyName()
                + (repeat.getEmoji() != null ? " · " + repeat.getEmoji() + " " + repeat.getUserFriendlyName() : "")
                + " · Página " + page + "/" + pages));
        children.add(ActionRow.of(
                Button.secondary("queue:" + (page - 1), Emoji.fromUnicode("◀️")).withDisabled(page <= 1),
                Button.secondary("queue:" + page + ":r", Emoji.fromUnicode("🔄")),
                Button.secondary("queue:" + (page + 1), Emoji.fromUnicode("▶️")).withDisabled(page >= pages)));
        return container(bot, guild, children);
    }
}
