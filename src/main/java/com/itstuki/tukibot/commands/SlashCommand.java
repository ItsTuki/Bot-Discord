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
import java.util.EnumSet;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * Base de todos los comandos slash. Reemplaza a Command/MusicCommand de
 * JDA-Utilities, que no funciona con JDA 6.
 */
public abstract class SlashCommand
{
    protected final Bot bot;
    /** Nombre base (inglés, como en JMusicBot). */
    protected String name;
    /** Nombre que ven los clientes de Discord en español. */
    protected String nameEs;
    /** Descripción en español. */
    protected String help;
    /** Descripción en inglés. */
    protected String helpEn;
    protected Category category = Category.GENERAL;
    protected boolean guildOnly = true;
    /** Para comandos de música: exige que haya algo sonando. */
    protected boolean bePlaying = false;
    /** Para comandos de música: exige estar en el canal de voz del bot (y lo conecta si hace falta). */
    protected boolean beListening = false;
    /** Para comandos de música: aplicar las comprobaciones de canal y voz. */
    protected boolean musicChecks = true;

    protected SlashCommand(Bot bot)
    {
        this.bot = bot;
    }

    public SlashCommandData build()
    {
        SlashCommandData data = Commands.slash(name, help);
        // Los nombres de comandos van en inglés para todos (/play, /skip...),
        // así funcionan igual sin importar el idioma de Discord. Las
        // descripciones y las opciones sí están en español.
        if(helpEn != null)
            data.setDescriptionLocalizations(Loc.en(helpEn));
        data.setContexts(guildOnly ? EnumSet.of(InteractionContextType.GUILD)
                : EnumSet.of(InteractionContextType.GUILD, InteractionContextType.BOT_DM));
        data.setIntegrationTypes(IntegrationType.GUILD_INSTALL);
        if(category.getDefaultPermission() != null)
            data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(category.getDefaultPermission()));
        configure(data);
        return data;
    }

    /** Agrega opciones o subcomandos. */
    protected void configure(SlashCommandData data) {}

    /** Ejecuta las comprobaciones de la categoría y luego el comando. */
    public final void run(CommandContext ctx)
    {
        if(guildOnly && ctx.guild() == null)
        {
            ctx.replyError("Este comando solo funciona en servidores.");
            return;
        }
        switch(category)
        {
            case OWNER:
                if(!Checks.isOwner(bot, ctx.user()))
                {
                    ctx.replyError("Solo el dueño del bot puede usar este comando.");
                    return;
                }
                break;
            case ADMIN:
                if(ctx.member() != null && !Checks.isAdmin(bot, ctx.member()))
                {
                    ctx.replyError("Necesitas el permiso **Gestionar servidor** para usar este comando.");
                    return;
                }
                break;
            case DJ:
                if(ctx.member() != null && !Checks.isDJ(bot, ctx.member()))
                {
                    var role = ctx.settings().getRole(ctx.guild());
                    ctx.replyError("Solo los DJ pueden usar este comando"
                            + (role == null ? " (no hay rol de DJ configurado; un admin puede ponerlo con `/config dj`)." : " (rol " + role.getAsMention() + ")."));
                    return;
                }
                break;
            default:
        }
        if((category == Category.MUSIC || category == Category.DJ) && musicChecks)
        {
            String error = Checks.checkMusic(bot, ctx.member(), ctx.channel(), bePlaying, beListening);
            if(error != null)
            {
                ctx.replyError(error);
                return;
            }
            ctx.handler().setAnnounceChannel(ctx.channel());
        }
        execute(ctx);
    }

    protected abstract void execute(CommandContext ctx);

    public void autocomplete(CommandAutoCompleteInteractionEvent event)
    {
        event.replyChoices().queue(s -> {}, f -> {});
    }

    public String getName() { return name; }
    public String getNameEs() { return nameEs == null ? name : nameEs; }
    public String getHelp() { return help; }
    public Category getCategory() { return category; }
}
