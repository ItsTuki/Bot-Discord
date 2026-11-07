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
package com.itstuki.tukibot.commands;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.audio.AudioHandler;
import com.itstuki.tukibot.settings.Settings;
import com.itstuki.tukibot.utils.FormatUtil;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

/**
 * Envoltura de un comando slash con atajos para leer opciones y responder.
 * Funciona igual si la respuesta ya fue diferida ("el bot está pensando...").
 */
public class CommandContext
{
    private final Bot bot;
    private final SlashCommandInteractionEvent event;
    private volatile boolean originalSent = false;

    public CommandContext(Bot bot, SlashCommandInteractionEvent event)
    {
        this.bot = bot;
        this.event = event;
    }

    public Bot bot() { return bot; }
    public SlashCommandInteractionEvent event() { return event; }
    public Guild guild() { return event.getGuild(); }
    public Member member() { return event.getMember(); }
    public User user() { return event.getUser(); }
    public InteractionHook hook() { return event.getHook(); }

    public GuildMessageChannel channel()
    {
        return event.isFromGuild() ? event.getGuildChannel() : null;
    }

    public Settings settings()
    {
        return bot.getSettingsManager().getSettings(guild());
    }

    public AudioHandler handler()
    {
        return bot.getPlayerManager().setUpHandler(guild());
    }

    // ---- opciones ----

    public OptionMapping option(String name)
    {
        return event.getOption(name);
    }

    public String string(String name, String def)
    {
        OptionMapping o = event.getOption(name);
        return o == null ? def : o.getAsString();
    }

    public long integer(String name, long def)
    {
        OptionMapping o = event.getOption(name);
        return o == null ? def : o.getAsLong();
    }

    public boolean bool(String name, boolean def)
    {
        OptionMapping o = event.getOption(name);
        return o == null ? def : o.getAsBoolean();
    }

    // ---- respuestas ----

    public void defer(boolean ephemeral)
    {
        if(!event.isAcknowledged())
            event.deferReply(ephemeral).queue();
    }

    public void reply(String text)
    {
        reply(text, false);
    }

    public void reply(String text, boolean ephemeral)
    {
        reply(MessageCreateData.fromContent(FormatUtil.truncate(FormatUtil.filter(text), 2000)), ephemeral, null);
    }

    public void replySuccess(String text)
    {
        reply(bot.getConfig().getSuccess() + " " + text, false);
    }

    public void replyWarning(String text)
    {
        reply(bot.getConfig().getWarning() + " " + text, false);
    }

    /** Los errores solo los ve quien usó el comando. */
    public void replyError(String text)
    {
        reply(bot.getConfig().getError() + " " + text, true);
    }

    public void reply(MessageCreateData data, boolean ephemeral, Consumer<Message> after)
    {
        Consumer<Message> success = after == null ? m -> {} : after;
        if(!event.isAcknowledged())
        {
            originalSent = true;
            if(after == null)
                event.reply(data).setEphemeral(ephemeral).queue(s -> {}, f -> {});
            else
                event.reply(data).setEphemeral(ephemeral)
                        .flatMap(InteractionHook::retrieveOriginal)
                        .queue(success, f -> {});
        }
        else if(!originalSent)
        {
            originalSent = true;
            event.getHook().editOriginal(MessageEditData.fromCreateData(data)).queue(success, f -> {});
        }
        else
            event.getHook().sendMessage(data).setEphemeral(ephemeral).queue(success, f -> {});
    }
}
