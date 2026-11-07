/*
 * Copyright 2022 John Grosh <john.a.grosh@gmail.com>.
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
package com.itstuki.tukibot.settings;

import com.itstuki.tukibot.queue.AbstractQueue;
import com.itstuki.tukibot.queue.FairQueue;
import com.itstuki.tukibot.queue.LinearQueue;
import com.itstuki.tukibot.queue.Queueable;
import com.itstuki.tukibot.queue.QueueSupplier;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Wolfgang Schwendtbauer
 */
public enum QueueType
{
    LINEAR("\u23E9", "Lineal", LinearQueue::new),     // ⏩
    FAIR("\uD83D\uDD22", "Justa", FairQueue::new);     // 🔢

    private final String userFriendlyName;
    private final String emoji;
    private final QueueSupplier supplier;

    QueueType(final String emoji, final String userFriendlyName, QueueSupplier supplier)
    {
        this.userFriendlyName = userFriendlyName;
        this.emoji = emoji;
        this.supplier = supplier;
    }

    public static List<String> getNames()
    {
        return Arrays.stream(QueueType.values())
                .map(type -> type.name().toLowerCase())
                .collect(Collectors.toList());
    }

    public <T extends Queueable> AbstractQueue<T> createInstance(AbstractQueue<T> previous)
    {
        return supplier.apply(previous);
    }

    public String getUserFriendlyName()
    {
        return userFriendlyName;
    }

    public String getEmoji()
    {
        return emoji;
    }
}
