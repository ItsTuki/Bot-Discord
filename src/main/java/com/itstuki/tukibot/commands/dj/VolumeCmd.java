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
package com.itstuki.tukibot.commands.dj;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.audio.AudioHandler;
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.music.PlayerActions;
import com.itstuki.tukibot.utils.FormatUtil;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class VolumeCmd extends SlashCommand
{
    public VolumeCmd(Bot bot)
    {
        super(bot);
        this.name = "volume";
        this.nameEs = "volumen";
        this.help = "Muestra o cambia el volumen (0-150)";
        this.helpEn = "Sets or shows the volume (0-150)";
        this.category = Category.DJ;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addOptions(Loc.option(OptionType.INTEGER, "level", "nivel", "Nuevo volumen entre 0 y 150", "New volume (0-150)", false)
                .setRequiredRange(0, 150));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        AudioHandler handler = ctx.handler();
        int volume = handler.getPlayer().getVolume();
        if(ctx.option("level") == null)
        {
            ctx.reply(FormatUtil.volumeIcon(volume) + " El volumen actual es `" + volume + "`");
            return;
        }
        int nvolume = PlayerActions.setVolume(handler, ctx.settings(), (int) ctx.integer("level", volume));
        bot.getNowplayingHandler().refresh(ctx.guild());
        ctx.reply(FormatUtil.volumeIcon(nvolume) + " Volumen cambiado de `" + volume + "` a `" + nvolume + "`");
    }
}
