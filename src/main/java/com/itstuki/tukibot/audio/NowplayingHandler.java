/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
 * Copyright 2026 ItsTuki (panel automático, estado del canal de voz y JDA 6).
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

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.entities.Pair;
import com.itstuki.tukibot.ui.Panels;
import com.itstuki.tukibot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

/**
 * Mantiene actualizado el panel de "Reproduciendo ahora" de cada servidor,
 * el estado del canal de voz y la actividad del bot.
 */
public class NowplayingHandler
{
    private final Bot bot;
    private final Map<Long, Pair<Long,Long>> lastNP = new ConcurrentHashMap<>(); // servidor -> canal, mensaje

    public NowplayingHandler(Bot bot)
    {
        this.bot = bot;
    }

    public void init()
    {
        bot.getThreadpool().scheduleWithFixedDelay(this::updateAll, 10, 10, TimeUnit.SECONDS);
    }

    public void setLastNPMessage(Message m)
    {
        if(!m.isFromGuild())
            return;
        Pair<Long,Long> previous = lastNP.put(m.getGuild().getIdLong(), new Pair<>(m.getChannel().getIdLong(), m.getIdLong()));
        // si había otro panel, se le quitan los botones para que no queden dos activos
        if(previous != null && previous.getValue() != m.getIdLong())
            deletePanel(m.getGuild(), previous);
    }

    public void clearLastNPMessage(Guild guild)
    {
        lastNP.remove(guild.getIdLong());
    }

    public boolean isPanel(Guild guild, long messageId)
    {
        Pair<Long,Long> pair = lastNP.get(guild.getIdLong());
        return pair != null && pair.getValue() == messageId;
    }

    private void updateAll()
    {
        try
        {
            for(Map.Entry<Long, Pair<Long,Long>> entry : lastNP.entrySet())
                update(entry.getKey(), entry.getValue());
        }
        catch(Exception ignored) {}
    }

    private void update(long guildId, Pair<Long,Long> pair)
    {
        Guild guild = bot.getJDA().getGuildById(guildId);
        GuildMessageChannel channel = guild == null ? null : guild.getChannelById(GuildMessageChannel.class, pair.getKey());
        AudioHandler handler = guild == null ? null : bot.getPlayerManager().getHandler(guild);
        if(channel == null || handler == null)
        {
            lastNP.remove(guildId);
            return;
        }
        boolean playing = handler.getPlayer().getPlayingTrack() != null;
        MessageCreateData data = playing ? Panels.nowPlaying(bot, guild, handler) : Panels.noMusic(bot, guild, handler);
        if(!playing)
            lastNP.remove(guildId);
        channel.editMessageById(pair.getValue(), MessageEditData.fromCreateData(data))
                .queue(m -> {}, t -> lastNP.remove(guildId, pair));
    }

    /** Actualiza el panel de inmediato (por ejemplo, después de pulsar un botón). */
    public void refresh(Guild guild)
    {
        Pair<Long,Long> pair = lastNP.get(guild.getIdLong());
        if(pair != null)
            update(guild.getIdLong(), pair);
    }

    private void deletePanel(Guild guild, Pair<Long,Long> pair)
    {
        GuildMessageChannel channel = guild.getChannelById(GuildMessageChannel.class, pair.getKey());
        if(channel != null)
            channel.deleteMessageById(pair.getValue()).queue(s -> {}, f -> {});
    }

    /** Envía un aviso al canal donde se usó el último comando de música. */
    public void announce(long guildId, String text)
    {
        GuildMessageChannel channel = announceChannel(guildId);
        if(channel != null)
            channel.sendMessage(FormatUtil.filter(text)).queue(s -> {}, f -> {});
    }

    private GuildMessageChannel announceChannel(long guildId)
    {
        Guild guild = bot.getJDA().getGuildById(guildId);
        AudioHandler handler = guild == null ? null : bot.getPlayerManager().getHandler(guild);
        if(handler == null || handler.getAnnounceChannelId() == 0L)
            return null;
        GuildMessageChannel channel = guild.getChannelById(GuildMessageChannel.class, handler.getAnnounceChannelId());
        return channel != null && channel.canTalk() ? channel : null;
    }

    // métodos tipo "evento"
    public void onTrackUpdate(long guildId, AudioTrack track)
    {
        Guild guild = bot.getJDA().getGuildById(guildId);
        if(guild == null)
            return;

        // actividad del bot
        if(bot.getConfig().getSongInStatus())
        {
            long playingGuilds = bot.getJDA().getGuilds().stream()
                    .filter(g -> g.getSelfMember().getVoiceState() != null && g.getSelfMember().getVoiceState().inAudioChannel()).count();
            if(track != null && playingGuilds <= 1)
                bot.getJDA().getPresence().setActivity(Activity.listening(FormatUtil.truncate(track.getInfo().title, 120)));
            else
                bot.resetGame();
        }

        // estado del canal de voz (la línea bajo el nombre del canal)
        setVoiceStatus(guild, track == null ? "" : "🎶 " + track.getInfo().title);

        // panel automático
        if(track != null && bot.getConfig().useAutoPanel())
        {
            GuildMessageChannel channel = announceChannel(guildId);
            AudioHandler handler = bot.getPlayerManager().getHandler(guild);
            if(channel != null && handler != null)
            {
                // se espera un poco para que la pista tenga posición y el bot esté conectado
                bot.getThreadpool().schedule(() ->
                    channel.sendMessage(Panels.nowPlaying(bot, guild, handler)).queue(this::setLastNPMessage, f -> {}),
                    1500, TimeUnit.MILLISECONDS);
            }
        }
        else if(track == null)
            refresh(guild);
    }

    public void setVoiceStatus(Guild guild, String status)
    {
        if(!bot.getConfig().useVoiceStatus())
            return;
        GuildVoiceState vs = guild.getSelfMember().getVoiceState();
        if(vs == null || vs.getChannel() == null || vs.getChannel().getType() != ChannelType.VOICE)
            return;
        VoiceChannel vc = vs.getChannel().asVoiceChannel();
        if(!guild.getSelfMember().hasPermission(vc, Permission.VOICE_SET_STATUS))
            return;
        String value = FormatUtil.truncate(status, 500);
        if(value.equals(vc.getStatus()))
            return;
        vc.modifyStatus(value).queue(s -> {}, f -> {});
    }

    public void onMessageDelete(Guild guild, long messageId)
    {
        Pair<Long,Long> pair = lastNP.get(guild.getIdLong());
        if(pair != null && pair.getValue() == messageId)
            lastNP.remove(guild.getIdLong());
    }
}
