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
import com.itstuki.tukibot.commands.Category;

/** Igual que /play, pero agrega la canción al inicio de la cola (solo DJ). */
public class PlaynextCmd extends PlayCmd
{
    public PlaynextCmd(Bot bot)
    {
        super(bot);
        this.name = "playnext";
        this.nameEs = "siguiente";
        this.help = "Agrega una canción para que suene a continuación";
        this.helpEn = "Plays a single song next";
        this.category = Category.DJ;
        this.next = true;
    }
}
