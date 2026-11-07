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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registra los comandos en Discord y reparte las interacciones (comandos,
 * autocompletado, menús contextuales, botones y menús de selección).
 */
public class CommandManager extends ListenerAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger("Comandos");

    private final Bot bot;
    private final Map<String, SlashCommand> commands = new LinkedHashMap<>();
    private final Map<String, MessageContextCommand> contextCommands = new LinkedHashMap<>();
    private final Map<String, ComponentHandler> components = new HashMap<>();
    private final Map<String, Long> commandIds = new ConcurrentHashMap<>();

    public CommandManager(Bot bot)
    {
        this.bot = bot;
    }

    public CommandManager addCommands(SlashCommand... cmds)
    {
        for(SlashCommand c : cmds)
            commands.put(c.getName(), c);
        return this;
    }

    public CommandManager addContextCommands(MessageContextCommand... cmds)
    {
        for(MessageContextCommand c : cmds)
            contextCommands.put(c.getName(), c);
        return this;
    }

    public CommandManager addComponentHandler(String prefix, ComponentHandler handler)
    {
        components.put(prefix, handler);
        return this;
    }

    public Collection<SlashCommand> getCommands()
    {
        return commands.values();
    }

    /** Mención clicable del comando, por ejemplo {@code </play:123>}. */
    public String mention(String commandPath)
    {
        String base = commandPath.split(" ")[0];
        Long id = commandIds.get(base);
        return id == null ? "`/" + commandPath + "`" : "</" + commandPath + ":" + id + ">";
    }

    /** Sube los comandos a Discord (al servidor de pruebas si está configurado). */
    public void upsertCommands(JDA jda)
    {
        List<CommandData> data = new ArrayList<>();
        commands.values().forEach(c -> data.add(c.build()));
        contextCommands.values().forEach(c -> data.add(c.build()));

        long devGuild = bot.getConfig().getDevGuild();
        Guild guild = devGuild == 0 ? null : jda.getGuildById(devGuild);
        if(devGuild != 0 && guild == null)
            LOG.warn("No encontré el servidor de pruebas " + devGuild + "; registro los comandos de forma global.");

        if(guild != null)
        {
            guild.updateCommands().addCommands(data).queue(this::storeIds,
                    t -> LOG.error("No se pudieron registrar los comandos en " + guild.getName() + ": " + t));
            // se limpian los globales para que no aparezcan duplicados
            jda.updateCommands().queue();
            LOG.info("Registrando " + data.size() + " comandos en el servidor de pruebas " + guild.getName());
        }
        else
        {
            jda.updateCommands().addCommands(data).queue(this::storeIds,
                    t -> LOG.error("No se pudieron registrar los comandos globales: " + t));
            LOG.info("Registrando " + data.size() + " comandos globales");
        }
    }

    private void storeIds(List<Command> list)
    {
        list.forEach(c -> commandIds.put(c.getName(), c.getIdLong()));
        LOG.info("Comandos registrados: " + list.size());
    }

    // ------------------------------------------------------------------

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        SlashCommand cmd = commands.get(event.getName());
        if(cmd == null)
        {
            event.reply(bot.getConfig().getError() + " Comando desconocido (puede que se esté actualizando).").setEphemeral(true).queue();
            return;
        }
        CommandContext ctx = new CommandContext(bot, event);
        try
        {
            cmd.run(ctx);
        }
        catch(Exception e)
        {
            LOG.error("Error ejecutando /" + event.getFullCommandName(), e);
            fail(event, e);
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event)
    {
        SlashCommand cmd = commands.get(event.getName());
        try
        {
            if(cmd != null)
                cmd.autocomplete(event);
            else
                event.replyChoices().queue();
        }
        catch(Exception e)
        {
            LOG.debug("Error en autocompletado de /" + event.getName(), e);
            if(!event.isAcknowledged())
                event.replyChoices().queue(s -> {}, f -> {});
        }
    }

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event)
    {
        MessageContextCommand cmd = contextCommands.get(event.getName());
        if(cmd == null)
            return;
        try
        {
            cmd.execute(event);
        }
        catch(Exception e)
        {
            LOG.error("Error en el menú contextual " + event.getName(), e);
            fail(event, e);
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event)
    {
        String[] parts = event.getComponentId().split(":");
        ComponentHandler handler = components.get(parts[0]);
        if(handler == null)
            return;
        try
        {
            handler.onButton(event, Arrays.copyOfRange(parts, 1, parts.length));
        }
        catch(Exception e)
        {
            LOG.error("Error en el botón " + event.getComponentId(), e);
            fail(event, e);
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event)
    {
        String[] parts = event.getComponentId().split(":");
        ComponentHandler handler = components.get(parts[0]);
        if(handler == null)
            return;
        try
        {
            handler.onSelect(event, Arrays.copyOfRange(parts, 1, parts.length));
        }
        catch(Exception e)
        {
            LOG.error("Error en el menú " + event.getComponentId(), e);
            fail(event, e);
        }
    }

    private void fail(IReplyCallback event, Exception e)
    {
        String msg = bot.getConfig().getError() + " Ocurrió un error inesperado: `" + e.getClass().getSimpleName() + "`";
        if(!event.isAcknowledged())
            event.reply(msg).setEphemeral(true).queue(s -> {}, f -> {});
        else
            event.getHook().sendMessage(msg).setEphemeral(true).queue(s -> {}, f -> {});
    }
}
