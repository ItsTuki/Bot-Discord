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

import net.dv8tion.jda.api.Permission;

/** Categorías de comandos (equivalen a las de JMusicBot). */
public enum Category
{
    GENERAL("General", "🎉", null),
    MUSIC("Música", "🎶", null),
    DJ("DJ", "🎧", null),
    ADMIN("Administración", "🛠️", Permission.MANAGE_SERVER),
    OWNER("Dueño del bot", "👑", Permission.ADMINISTRATOR);

    private final String title;
    private final String emoji;
    private final Permission defaultPermission;

    Category(String title, String emoji, Permission defaultPermission)
    {
        this.title = title;
        this.emoji = emoji;
        this.defaultPermission = defaultPermission;
    }

    public String getTitle() { return title; }
    public String getEmoji() { return emoji; }
    /** Permiso que Discord exige por defecto para ver el comando (los admins del servidor pueden cambiarlo). */
    public Permission getDefaultPermission() { return defaultPermission; }
}
