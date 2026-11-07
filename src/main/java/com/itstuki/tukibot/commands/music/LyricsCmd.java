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
import com.itstuki.tukibot.audio.AudioHandler;
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.lyrics.LyricsClient;
import com.itstuki.tukibot.ui.Panels;
import com.itstuki.tukibot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class LyricsCmd extends SlashCommand
{
    public LyricsCmd(Bot bot)
    {
        super(bot);
        this.name = "lyrics";
        this.nameEs = "letra";
        this.help = "Muestra la letra de la canción actual o de la que indiques";
        this.helpEn = "Shows the lyrics of a song";
        this.category = Category.MUSIC;
        this.musicChecks = false;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addOptions(Loc.option(OptionType.STRING, "song", "cancion", "Nombre de la canción (por defecto, la que está sonando)", "Song name", false));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        String title = ctx.string("song", null);
        String artist = null;
        if(title == null || title.isBlank())
        {
            AudioHandler handler = bot.getPlayerManager().getHandler(ctx.guild());
            if(handler == null || !handler.isMusicPlaying(ctx.guild().getJDA()))
            {
                ctx.replyError("No hay música sonando. Indica el nombre de la canción en la opción **cancion**.");
                return;
            }
            AudioTrack track = handler.getPlayer().getPlayingTrack();
            title = track.getInfo().title;
            artist = track.getInfo().author;
        }
        ctx.defer(false);
        send(bot, ctx.guild(), ctx.hook(), title, artist);
    }

    /** Busca la letra y la envía (también lo usa el botón del panel). */
    public static void send(Bot bot, Guild guild, InteractionHook hook, String title, String artist)
    {
        bot.getLyrics().getLyrics(title, artist).thenAccept(lyrics -> {
            if(lyrics == null)
            {
                hook.editOriginal(bot.getConfig().getError() + " No encontré la letra de `" + FormatUtil.truncate(title, 150).replace("`", "")
                        + "`. Prueba escribiendo el nombre a mano en " + bot.getCommands().mention("lyrics") + ".").queue();
                return;
            }
            List<MessageEmbed> embeds = build(bot, guild, lyrics);
            hook.editOriginalEmbeds(embeds.get(0)).queue();
            for(int i = 1; i < embeds.size() && i < 5; i++)
                hook.sendMessageEmbeds(embeds.get(i)).queue();
        });
    }

    private static List<MessageEmbed> build(Bot bot, Guild guild, LyricsClient.Lyrics lyrics)
    {
        List<MessageEmbed> out = new ArrayList<>();
        String content = lyrics.content();
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(Panels.accent(bot, guild))
                .setAuthor(lyrics.artist().isBlank() ? null : FormatUtil.truncate(lyrics.artist(), 250))
                .setTitle(FormatUtil.truncate(lyrics.title(), 250), lyrics.url());
        while(content.length() > 4000)
        {
            int index = content.lastIndexOf("\n\n", 4000);
            if(index <= 0)
                index = content.lastIndexOf("\n", 4000);
            if(index <= 0)
                index = 4000;
            out.add(eb.setDescription(content.substring(0, index).trim()).build());
            content = content.substring(index).trim();
            eb = new EmbedBuilder().setColor(Panels.accent(bot, guild));
        }
        out.add(eb.setDescription(content).setFooter("Letra de LRCLIB").build());
        return out;
    }
}
