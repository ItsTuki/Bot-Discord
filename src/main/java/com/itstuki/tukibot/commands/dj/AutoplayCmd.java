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
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.settings.Settings;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class AutoplayCmd extends SlashCommand
{
    public AutoplayCmd(Bot bot)
    {
        super(bot);
        this.name = "autoplay";
        this.nameEs = "autoplay";
        this.help = "Al terminar la cola sigue con canciones parecidas (modo radio)";
        this.helpEn = "Keeps playing similar songs when the queue ends (radio mode)";
        this.category = Category.DJ;
        this.musicChecks = false;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addOptions(Loc.option(OptionType.BOOLEAN, "enabled", "activado", "Activar o desactivar (vacío = alternar)", "Turn on or off (empty = toggle)", false));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        Settings settings = ctx.settings();
        boolean value = ctx.option("enabled") == null ? !settings.isAutoplay() : ctx.bool("enabled", false);
        settings.setAutoplay(value);
        bot.getNowplayingHandler().refresh(ctx.guild());
        ctx.replySuccess(value
                ? "📻 Autoplay **activado**: cuando se acabe la cola seguiré con canciones parecidas a la última."
                : "Autoplay **desactivado**.");
    }
}
