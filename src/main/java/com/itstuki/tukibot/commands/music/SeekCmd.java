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
package com.itstuki.tukibot.commands.music;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.audio.RequestMetadata;
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.utils.FormatUtil;
import com.itstuki.tukibot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.LoggerFactory;

public class SeekCmd extends SlashCommand
{
    public SeekCmd(Bot bot)
    {
        super(bot);
        this.name = "seek";
        this.nameEs = "adelantar";
        this.help = "Salta a un momento de la canción (ej: 1:30, +30, -10, 2m15s)";
        this.helpEn = "Seeks the current song (e.g. 1:30, +30, -10, 2m15s)";
        this.category = Category.MUSIC;
        this.beListening = true;
        this.bePlaying = true;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addOptions(Loc.option(OptionType.STRING, "time", "tiempo",
                "Momento exacto (1:02:23, 90) o relativo (+1:10, -30, +90s)", "Absolute or relative time", true));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        AudioTrack playing = ctx.handler().getPlayer().getPlayingTrack();
        if(!playing.isSeekable())
        {
            ctx.replyError("Esta canción no permite adelantar ni retroceder.");
            return;
        }
        RequestMetadata rm = playing.getUserData(RequestMetadata.class);
        if(!Checks.isDJ(bot, ctx.member()) && (rm == null || rm.getOwner() != ctx.user().getIdLong()))
        {
            ctx.replyError("No puedes mover **" + FormatUtil.escape(playing.getInfo().title) + "** porque no la agregaste tú.");
            return;
        }
        TimeUtil.SeekTime seekTime = TimeUtil.parseTime(ctx.string("time", "").trim());
        if(seekTime == null)
        {
            ctx.replyError("Formato inválido. Ejemplos: `1:02:23` `+1:10` `-90` `1h10m` `+90s`");
            return;
        }
        long target = seekTime.relative ? playing.getPosition() + seekTime.milliseconds : seekTime.milliseconds;
        target = Math.max(0, target);
        if(target > playing.getDuration())
        {
            ctx.replyError("No puedo ir a `" + TimeUtil.formatTime(target) + "` porque la canción dura `" + TimeUtil.formatTime(playing.getDuration()) + "`.");
            return;
        }
        try
        {
            playing.setPosition(target);
        }
        catch(Exception e)
        {
            LoggerFactory.getLogger("Seek").warn("Error al mover la pista " + playing.getIdentifier(), e);
            ctx.replyError("Ocurrió un error al mover la canción: " + e.getMessage());
            return;
        }
        bot.getNowplayingHandler().refresh(ctx.guild());
        ctx.replySuccess("Listo, ahora en `" + TimeUtil.formatTime(target) + " / " + TimeUtil.formatTime(playing.getDuration()) + "`.");
    }
}
