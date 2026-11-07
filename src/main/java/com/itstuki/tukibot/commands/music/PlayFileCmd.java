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
import com.itstuki.tukibot.commands.*;
import com.itstuki.tukibot.music.TrackLoader;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/** Reproduce un archivo de audio adjunto. */
public class PlayFileCmd extends SlashCommand
{
    private final TrackLoader loader;

    public PlayFileCmd(Bot bot)
    {
        super(bot);
        this.loader = new TrackLoader(bot);
        this.name = "playfile";
        this.help = "Reproduce un archivo de audio que adjuntes (mp3, ogg, wav, flac...)";
        this.helpEn = "Plays an attached audio file";
        this.category = Category.MUSIC;
        this.beListening = true;
    }

    @Override
    protected void configure(SlashCommandData data)
    {
        data.addOptions(Loc.option(OptionType.ATTACHMENT, "file", "archivo", "Archivo de audio", "Audio file", true));
    }

    @Override
    protected void execute(CommandContext ctx)
    {
        Message.Attachment att = ctx.option("file").getAsAttachment();
        ctx.defer(false);
        loader.load(ctx.guild(), ctx.member(), ctx.hook(), att.getUrl(), false);
    }
}
