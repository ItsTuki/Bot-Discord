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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class ForceRemoveCmd extends SlashCommand
{
    public ForceRemoveCmd(Bot bot)
    {
        super(bot);
        this.name = "forceremove";
        this.nameEs = "quitar-de";
        this.help = "Quita de la cola todas las canciones de un usuario";
        this.helpEn = "Removes all entries by a user from the queue";
        this.category = Category.DJ;
        this.bePlaying = true;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addOptions(Loc.option(OptionType.USER, "user", "usuario", "Usuario cuyas canciones se quitarán", "User whose songs will be removed", true));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        if(ctx.handler().getQueue().isEmpty())
        {
            ctx.replyError("No hay nada en la cola.");
            return;
        }
        User target = ctx.option("user").getAsUser();
        int count = ctx.handler().getQueue().removeAll(target.getIdLong());
        if(count == 0)
            ctx.replyWarning(target.getAsMention() + " no tiene canciones en la cola.");
        else
            ctx.replySuccess("Quité `" + count + "` canciones de " + target.getAsMention() + ".");
    }
}
