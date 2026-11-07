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
package com.itstuki.tukibot.interactions;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.commands.Checks;
import com.itstuki.tukibot.commands.Loc;
import com.itstuki.tukibot.commands.MessageContextCommand;
import com.itstuki.tukibot.music.TrackLoader;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Nuevo: clic derecho en un mensaje > Apps > "Reproducir en voz". Agrega a
 * la cola el primer enlace o archivo de audio del mensaje.
 */
public class PlayInVoiceContext implements MessageContextCommand
{
    private static final Pattern LINK = Pattern.compile("https?://\\S+");
    private static final String NAME = "Play in voice";

    private final Bot bot;
    private final TrackLoader loader;

    public PlayInVoiceContext(Bot bot)
    {
        this.bot = bot;
        this.loader = new TrackLoader(bot);
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public CommandData build()
    {
        return Commands.message(NAME)
                .setNameLocalizations(Loc.es("Reproducir en voz"))
                .setContexts(EnumSet.of(InteractionContextType.GUILD))
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL);
    }

    @Override
    public void execute(MessageContextInteractionEvent event)
    {
        if(event.getGuild() == null || event.getMember() == null)
            return;
        Message msg = event.getTarget();
        String query = null;
        for(Message.Attachment att : msg.getAttachments())
            if(att.getContentType() == null || att.getContentType().startsWith("audio") || att.getContentType().startsWith("video"))
            {
                query = att.getUrl();
                break;
            }
        if(query == null)
        {
            Matcher m = LINK.matcher(msg.getContentRaw());
            if(m.find())
                query = m.group().replaceAll("[>)\\]]+$", "");
        }
        if(query == null)
        {
            event.reply(bot.getConfig().getError() + " Ese mensaje no tiene ningún enlace ni archivo de audio.").setEphemeral(true).queue();
            return;
        }
        String error = Checks.checkMusic(bot, event.getMember(), event.getGuildChannel(), false, true);
        if(error != null)
        {
            event.reply(bot.getConfig().getError() + " " + error).setEphemeral(true).queue();
            return;
        }
        bot.getPlayerManager().setUpHandler(event.getGuild()).setAnnounceChannel(event.getGuildChannel());
        event.deferReply().queue();
        loader.load(event.getGuild(), event.getMember(), event.getHook(), query, false);
    }
}
