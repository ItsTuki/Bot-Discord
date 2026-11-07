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
package com.itstuki.tukibot.commands.general;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.ui.Panels;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;

public class HelpCmd extends SlashCommand
{
    public HelpCmd(Bot bot)
    {
        super(bot);
        this.name = "help";
        this.nameEs = "ayuda";
        this.help = "Muestra todos los comandos del bot";
        this.helpEn = "Shows all the bot commands";
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of("## " + bot.getConfig().getSuccess() + " Comandos de " + ctx.event().getJDA().getSelfUser().getName()
                + "\nToca un comando para usarlo. Los paneles de música también tienen botones."));
        for(Category cat : Category.values())
        {
            StringBuilder sb = new StringBuilder();
            for(SlashCommand c : bot.getCommands().getCommands())
            {
                if(c.getCategory() != cat)
                    continue;
                if(cat == Category.OWNER && !Checks.isOwner(bot, ctx.user()))
                    continue;
                sb.append(bot.getCommands().mention(c.getName())).append(" — ").append(c.getHelp()).append("\n");
            }
            if(sb.isEmpty())
                continue;
            children.add(Separator.createDivider(Separator.Spacing.SMALL));
            children.add(TextDisplay.of("### " + cat.getEmoji() + " " + cat.getTitle() + "\n" + sb.toString().trim()));
        }
        children.add(Separator.createDivider(Separator.Spacing.SMALL));
        children.add(TextDisplay.of("-# Clic derecho en un mensaje con un enlace > Apps > **Reproducir en voz** para ponerlo en la cola."));
        ctx.reply(Panels.container(bot, ctx.guild(), children), true, null);
    }
}
