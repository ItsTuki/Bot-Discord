/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
 * Copyright 2026 ItsTuki (lógica compartida entre comandos y botones).
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
package com.itstuki.tukibot.music;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.audio.AudioHandler;
import com.itstuki.tukibot.audio.RequestMetadata;
import com.itstuki.tukibot.settings.RepeatMode;
import com.itstuki.tukibot.settings.Settings;
import com.itstuki.tukibot.utils.FormatUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;

/**
 * Acciones del reproductor que se usan tanto desde los comandos como desde
 * los botones del panel.
 */
public final class PlayerActions
{
    private PlayerActions() {}

    /** Votación para saltar (lógica de SkipCmd de JMusicBot). */
    public static String voteSkip(Bot bot, Member member, AudioHandler handler)
    {
        RequestMetadata rm = handler.getRequestMetadata();
        String title = FormatUtil.escape(handler.getPlayer().getPlayingTrack().getInfo().title);
        double skipRatio = bot.getSettingsManager().getSettings(member.getGuild()).getSkipRatio();
        if(skipRatio == -1)
            skipRatio = bot.getConfig().getSkipRatio();

        if(member.getIdLong() == rm.getOwner() || skipRatio == 0)
        {
            handler.getPlayer().stopTrack();
            return bot.getConfig().getSuccess() + " " + member.getAsMention() + " saltó **" + title + "**";
        }

        GuildVoiceState self = member.getGuild().getSelfMember().getVoiceState();
        if(self == null || self.getChannel() == null)
            return bot.getConfig().getError() + " No estoy en un canal de voz.";
        int listeners = (int) self.getChannel().getMembers().stream()
                .filter(m -> !m.getUser().isBot() && m.getVoiceState() != null && !m.getVoiceState().isDeafened()).count();
        String msg;
        if(handler.getVotes().contains(member.getId()))
            msg = bot.getConfig().getWarning() + " Ya votaste para saltar esta canción `[";
        else
        {
            msg = bot.getConfig().getSuccess() + " " + member.getAsMention() + " votó para saltar la canción `[";
            handler.getVotes().add(member.getId());
        }
        int skippers = (int) self.getChannel().getMembers().stream()
                .filter(m -> handler.getVotes().contains(m.getUser().getId())).count();
        int required = (int) Math.ceil(listeners * skipRatio);
        msg += skippers + " votos, se necesitan " + required + "/" + listeners + "]`";
        if(skippers >= required)
        {
            msg += "\n" + bot.getConfig().getSuccess() + " Salté **" + title + "** "
                    + (rm.getOwner() == 0L ? "(reproducción automática)" : "(pedida por **" + FormatUtil.escape(FormatUtil.formatUsername(rm.user)) + "**)");
            handler.getPlayer().stopTrack();
        }
        return msg;
    }

    public static String forceSkip(Bot bot, AudioHandler handler)
    {
        RequestMetadata rm = handler.getRequestMetadata();
        String msg = bot.getConfig().getSuccess() + " Salté **" + FormatUtil.escape(handler.getPlayer().getPlayingTrack().getInfo().title) + "** "
                + (rm.getOwner() == 0L ? "(reproducción automática)" : "(pedida por **" + FormatUtil.escape(FormatUtil.formatUsername(rm.user)) + "**)");
        handler.getPlayer().stopTrack();
        return msg;
    }

    public static String stop(Bot bot, Guild guild, AudioHandler handler)
    {
        handler.stopAndClear();
        bot.closeAudioConnection(guild.getIdLong());
        return bot.getConfig().getSuccess() + " Detuve la música y vacié la cola.";
    }

    public static String shuffle(Bot bot, Member member, AudioHandler handler)
    {
        int s = handler.getQueue().shuffle(member.getIdLong());
        return switch(s)
        {
            case 0 -> bot.getConfig().getError() + " No tienes canciones en la cola para mezclar.";
            case 1 -> bot.getConfig().getWarning() + " Solo tienes una canción en la cola.";
            default -> bot.getConfig().getSuccess() + " Mezclaste tus " + s + " canciones.";
        };
    }

    public static RepeatMode cycleRepeat(Settings settings)
    {
        RepeatMode next = switch(settings.getRepeatMode())
        {
            case OFF -> RepeatMode.ALL;
            case ALL -> RepeatMode.SINGLE;
            case SINGLE -> RepeatMode.OFF;
        };
        settings.setRepeatMode(next);
        return next;
    }

    public static int setVolume(AudioHandler handler, Settings settings, int volume)
    {
        int v = Math.max(0, Math.min(150, volume));
        handler.getPlayer().setVolume(v);
        settings.setVolume(v);
        return v;
    }
}
