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
package com.itstuki.tukibot.commands.owner;

import com.itstuki.tukibot.Bot;
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.utils.OtherUtil;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

/**
 * Comandos del dueño del bot: setgame, setstatus, setname, setavatar, debug
 * y shutdown de JMusicBot como subcomandos de /owner. Se quitó eval por
 * seguridad (y porque Nashorn ya no existe en Java moderno).
 */
public class OwnerCmd extends SlashCommand
{
    public OwnerCmd(Bot bot)
    {
        super(bot);
        this.name = "owner";
        this.nameEs = "dueno";
        this.help = "Comandos del dueño del bot";
        this.helpEn = "Bot owner commands";
        this.category = Category.OWNER;
        this.guildOnly = false;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addSubcommands(
                Loc.sub("activity", "actividad", "Cambia la actividad del bot", "Sets the bot activity")
                        .addOptions(
                                Loc.option(OptionType.STRING, "type", "tipo", "Tipo de actividad", "Activity type", true)
                                        .addChoice("Jugando", "playing").addChoice("Escuchando", "listening")
                                        .addChoice("Viendo", "watching").addChoice("Compitiendo en", "competing")
                                        .addChoice("Transmitiendo (Twitch)", "streaming").addChoice("Estado personalizado", "custom")
                                        .addChoice("Ninguna", "none").addChoice("Por defecto", "default"),
                                Loc.option(OptionType.STRING, "text", "texto", "Texto de la actividad", "Activity text", false),
                                Loc.option(OptionType.STRING, "twitch", "twitch", "Usuario de Twitch (solo para Transmitiendo)", "Twitch username", false)),
                Loc.sub("status", "estado", "Cambia el estado del bot", "Sets the bot status")
                        .addOptions(Loc.option(OptionType.STRING, "status", "estado", "Estado", "Status", true)
                                .addChoice("En línea", "ONLINE").addChoice("Ausente", "IDLE")
                                .addChoice("No molestar", "DND").addChoice("Invisible", "INVISIBLE")),
                Loc.sub("name", "nombre", "Cambia el nombre de usuario del bot", "Sets the bot username")
                        .addOptions(Loc.option(OptionType.STRING, "name", "nombre", "Nuevo nombre", "New name", true).setRequiredLength(2, 32)),
                Loc.sub("avatar", "avatar", "Cambia la foto del bot (archivo o enlace)", "Sets the bot avatar")
                        .addOptions(Loc.option(OptionType.ATTACHMENT, "image", "imagen", "Imagen nueva", "New image", false),
                                Loc.option(OptionType.STRING, "url", "url", "Enlace a la imagen", "Image URL", false)),
                Loc.sub("debug", "debug", "Información técnica del bot", "Shows debug info"),
                Loc.sub("shutdown", "apagar", "Apaga el bot de forma segura", "Safely shuts down the bot"));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        String sub = ctx.event().getSubcommandName();
        switch(sub == null ? "" : sub)
        {
            case "activity" -> activity(ctx);
            case "status" -> {
                OnlineStatus status = OnlineStatus.valueOf(ctx.string("status", "ONLINE"));
                ctx.event().getJDA().getPresence().setStatus(status);
                ctx.reply(bot.getConfig().getSuccess() + " Estado cambiado a `" + status.name() + "`", true);
            }
            case "name" -> {
                String newName = ctx.string("name", "");
                ctx.defer(true);
                ctx.event().getJDA().getSelfUser().getManager().setName(newName).queue(
                        s -> ctx.hook().editOriginal(bot.getConfig().getSuccess() + " Nombre cambiado a **" + newName + "**").queue(),
                        t -> ctx.hook().editOriginal(bot.getConfig().getError() + " No se pudo cambiar el nombre (Discord limita los cambios por hora): " + t.getMessage()).queue());
            }
            case "avatar" -> avatar(ctx);
            case "debug" -> debug(ctx);
            case "shutdown" -> {
                ctx.event().reply(bot.getConfig().getWarning() + " Apagando...").setEphemeral(true).queue(s -> bot.shutdown(), f -> bot.shutdown());
            }
            default -> ctx.replyError("Subcomando desconocido.");
        }
    }

    private void activity(CommandContext ctx)
    {
        String type = ctx.string("type", "default");
        String text = ctx.string("text", "").trim();
        Activity activity;
        switch(type)
        {
            case "none" -> activity = null;
            case "default" -> activity = bot.getDefaultGame();
            case "streaming" -> {
                String twitch = ctx.string("twitch", bot.getConfig().getTwitch());
                activity = Activity.streaming(OtherUtil.makeNonEmpty(text), "https://twitch.tv/" + twitch);
            }
            default -> {
                if(text.isEmpty())
                {
                    ctx.replyError("Escribe el texto de la actividad en la opción **texto**.");
                    return;
                }
                activity = switch(type)
                {
                    case "playing" -> Activity.playing(text);
                    case "listening" -> Activity.listening(text);
                    case "watching" -> Activity.watching(text);
                    case "competing" -> Activity.competing(text);
                    default -> Activity.customStatus(text);
                };
            }
        }
        ctx.event().getJDA().getPresence().setActivity(activity);
        ctx.reply(bot.getConfig().getSuccess() + (activity == null ? " Quité la actividad del bot." : " Actividad cambiada a **" + activity.getName() + "**."), true);
    }

    private void avatar(CommandContext ctx)
    {
        OptionMapping image = ctx.option("image");
        String url = image != null ? image.getAsAttachment().getUrl() : ctx.string("url", null);
        if(url == null)
        {
            ctx.replyError("Adjunta una imagen o escribe un enlace.");
            return;
        }
        ctx.defer(true);
        try(InputStream s = OtherUtil.imageFromUrl(url))
        {
            if(s == null)
            {
                ctx.hook().editOriginal(bot.getConfig().getError() + " Enlace inválido o inaccesible.").queue();
                return;
            }
            ctx.event().getJDA().getSelfUser().getManager().setAvatar(Icon.from(s)).queue(
                    v -> ctx.hook().editOriginal(bot.getConfig().getSuccess() + " Avatar cambiado.").queue(),
                    t -> ctx.hook().editOriginal(bot.getConfig().getError() + " No se pudo cambiar el avatar: " + t.getMessage()).queue());
        }
        catch(IOException | IllegalArgumentException e)
        {
            ctx.hook().editOriginal(bot.getConfig().getError() + " No pude leer esa imagen.").queue();
        }
    }

    private void debug(CommandContext ctx)
    {
        String[] props = {"java.version", "java.vm.name", "java.runtime.version", "os.arch", "os.name"};
        StringBuilder sb = new StringBuilder("Propiedades del sistema:");
        for(String key : props)
            sb.append("\n  ").append(key).append(" = ").append(System.getProperty(key));
        var cfg = bot.getConfig();
        sb.append("\n\nTukiBot:")
                .append("\n  Versión = ").append(OtherUtil.getCurrentVersion())
                .append("\n  Dueño = ").append(cfg.getOwnerId())
                .append("\n  MaxTime = ").append(cfg.getMaxSeconds())
                .append("\n  NPImages = ").append(cfg.useNPImages())
                .append("\n  NPAuto = ").append(cfg.useAutoPanel())
                .append("\n  VCStatus = ").append(cfg.useVoiceStatus())
                .append("\n  SongInStatus = ").append(cfg.getSongInStatus())
                .append("\n  StayInChannel = ").append(cfg.getStay())
                .append("\n  YouTube OAuth = ").append(cfg.useYoutubeOauth())
                .append("\n  DevGuild = ").append(cfg.getDevGuild());
        sb.append("\n\nDependencias:")
                .append("\n  JDA = ").append(JDAInfo.VERSION)
                .append("\n  Lavaplayer = ").append(PlayerLibrary.VERSION)
                .append("\n  youtube-source = ").append(dev.lavalink.youtube.YoutubeSource.VERSION);
        long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long used = total - (Runtime.getRuntime().freeMemory() / 1024 / 1024);
        sb.append("\n\nMemoria: ").append(used).append(" / ").append(total).append(" MB");
        var jda = ctx.event().getJDA();
        sb.append("\n\nDiscord:")
                .append("\n  ID = ").append(jda.getSelfUser().getId())
                .append("\n  Servidores = ").append(jda.getGuildCache().size())
                .append("\n  Conexiones de voz = ").append(jda.getGuilds().stream()
                        .filter(g -> g.getSelfMember().getVoiceState() != null && g.getSelfMember().getVoiceState().inAudioChannel()).count())
                .append("\n  Ping del gateway = ").append(jda.getGatewayPing()).append(" ms");
        ctx.reply(new MessageCreateBuilder().setContent("Información de depuración:")
                .setFiles(FileUpload.fromData(sb.toString().getBytes(StandardCharsets.UTF_8), "debug.txt")).build(), true, null);
    }
}
