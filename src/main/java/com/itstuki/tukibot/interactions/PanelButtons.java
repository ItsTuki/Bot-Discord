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
import com.itstuki.tukibot.audio.AudioHandler;
import com.itstuki.tukibot.commands.Checks;
import com.itstuki.tukibot.commands.ComponentHandler;
import com.itstuki.tukibot.commands.music.LyricsCmd;
import com.itstuki.tukibot.music.PlayerActions;
import com.itstuki.tukibot.settings.Settings;
import com.itstuki.tukibot.ui.Panels;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

/**
 * Botones del panel "Reproduciendo ahora" (np:*) y de la cola (queue:*).
 * Reemplazan las reacciones que usaba JMusicBot.
 */
public class PanelButtons implements ComponentHandler
{
    private final Bot bot;

    public PanelButtons(Bot bot)
    {
        this.bot = bot;
    }

    private void error(ButtonInteractionEvent event, String text)
    {
        event.reply(bot.getConfig().getError() + " " + text).setEphemeral(true).queue();
    }

    @Override
    public void onButton(ButtonInteractionEvent event, String[] args)
    {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if(guild == null || member == null || args.length == 0)
        {
            event.deferEdit().queue();
            return;
        }
        if(event.getComponentId().startsWith("queue:"))
        {
            queue(event, args);
            return;
        }

        String action = args[0];
        AudioHandler handler = bot.getPlayerManager().getHandler(guild);
        if(handler == null || !handler.isMusicPlaying(guild.getJDA()))
        {
            event.editMessage(MessageEditData.fromCreateData(Panels.noMusic(bot, guild, handler))).queue();
            return;
        }

        // ver la cola o la letra no exige estar en el canal de voz
        if(action.equals("queue"))
        {
            event.reply(Panels.queue(bot, guild, handler, 1)).setEphemeral(true).queue();
            return;
        }
        if(action.equals("lyrics"))
        {
            AudioTrack track = handler.getPlayer().getPlayingTrack();
            event.deferReply(true).queue();
            LyricsCmd.send(bot, guild, event.getHook(), track.getInfo().title, track.getInfo().author);
            return;
        }

        String check = Checks.checkMusic(bot, member, event.getGuildChannel(), true, true);
        if(check != null)
        {
            error(event, check);
            return;
        }
        handler.setAnnounceChannel(event.getGuildChannel());

        switch(action)
        {
            case "skip" -> {
                event.reply(PlayerActions.voteSkip(bot, member, handler)).queue();
                return;
            }
            case "shuffle" -> {
                event.reply(PlayerActions.shuffle(bot, member, handler)).setEphemeral(true).queue();
                bot.getNowplayingHandler().refresh(guild);
                return;
            }
            default -> {}
        }

        // el resto son acciones de DJ
        if(!Checks.isDJ(bot, member))
        {
            error(event, "Solo los DJ pueden usar este botón.");
            return;
        }
        Settings settings = bot.getSettingsManager().getSettings(guild);
        switch(action)
        {
            case "pause" -> handler.getPlayer().setPaused(!handler.getPlayer().isPaused());
            case "stop" -> {
                PlayerActions.stop(bot, guild, handler);
                bot.getNowplayingHandler().clearLastNPMessage(guild);
                event.editMessage(MessageEditData.fromCreateData(Panels.simple(bot, guild,
                        "### ⏹ Música detenida\n" + member.getAsMention() + " detuvo la música y vació la cola."))).queue();
                return;
            }
            case "repeat" -> PlayerActions.cycleRepeat(settings);
            case "volup" -> PlayerActions.setVolume(handler, settings, handler.getPlayer().getVolume() + 10);
            case "voldown" -> PlayerActions.setVolume(handler, settings, handler.getPlayer().getVolume() - 10);
            case "autoplay" -> settings.setAutoplay(!settings.isAutoplay());
            default -> {}
        }
        event.editMessage(MessageEditData.fromCreateData(Panels.nowPlaying(bot, guild, handler))).queue();
    }

    private void queue(ButtonInteractionEvent event, String[] args)
    {
        Guild guild = event.getGuild();
        AudioHandler handler = bot.getPlayerManager().getHandler(guild);
        if(handler == null)
        {
            event.editMessage(MessageEditData.fromCreateData(Panels.noMusic(bot, guild, null))).queue();
            return;
        }
        int page;
        try
        {
            page = Integer.parseInt(args[0]);
        }
        catch(NumberFormatException e)
        {
            page = 1;
        }
        event.editMessage(MessageEditData.fromCreateData(Panels.queue(bot, guild, handler, page))).queue();
    }
}
