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
package com.itstuki.tukibot.userplaylist;

import com.itstuki.tukibot.utils.OtherUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Playlists personales de cada usuario (/myplaylist). Se guardan en
 * userplaylists.json junto al bot y sirven en cualquier servidor.
 */
public class UserPlaylists
{
    private static final Logger LOG = LoggerFactory.getLogger("UserPlaylists");
    private static final String FILE = "userplaylists.json";
    public static final int MAX_PLAYLISTS = 20;
    public static final int MAX_TRACKS = 500;
    public static final int MAX_NAME = 40;

    public record Entry(String title, String uri) {}

    /** usuario → (nombre → canciones), respetando el orden en que se crearon. */
    private final Map<Long, Map<String, List<Entry>>> data = new LinkedHashMap<>();

    public UserPlaylists()
    {
        Path path = OtherUtil.getPath(FILE);
        if(!Files.exists(path))
            return;
        try
        {
            JSONObject root = new JSONObject(Files.readString(path, StandardCharsets.UTF_8));
            for(String user : root.keySet())
            {
                Map<String, List<Entry>> lists = new LinkedHashMap<>();
                JSONObject obj = root.getJSONObject(user);
                for(String name : obj.keySet())
                {
                    List<Entry> entries = new ArrayList<>();
                    JSONArray arr = obj.getJSONArray(name);
                    for(int i = 0; i < arr.length(); i++)
                    {
                        JSONObject e = arr.getJSONObject(i);
                        entries.add(new Entry(e.optString("title", "?"), e.getString("uri")));
                    }
                    lists.put(name, entries);
                }
                data.put(Long.parseLong(user), lists);
            }
            LOG.info("Cargué las playlists personales de " + data.size() + " usuarios.");
        }
        catch(Exception e)
        {
            LOG.error("No pude leer " + FILE + "; se empieza vacío (el archivo no se toca hasta el próximo cambio).", e);
        }
    }

    public static String cleanName(String name)
    {
        if(name == null)
            return "";
        String n = name.trim().replaceAll("\\s+", " ");
        return n.length() > MAX_NAME ? n.substring(0, MAX_NAME) : n;
    }

    private String findKey(Map<String, List<Entry>> lists, String name)
    {
        for(String key : lists.keySet())
            if(key.equalsIgnoreCase(name))
                return key;
        return null;
    }

    public synchronized List<String> names(long user)
    {
        Map<String, List<Entry>> lists = data.get(user);
        return lists == null ? List.of() : List.copyOf(lists.keySet());
    }

    public synchronized int size(long user, String name)
    {
        List<Entry> l = get(user, name);
        return l == null ? 0 : l.size();
    }

    /** Copia de la playlist, o null si no existe. */
    public synchronized List<Entry> get(long user, String name)
    {
        Map<String, List<Entry>> lists = data.get(user);
        if(lists == null)
            return null;
        String key = findKey(lists, name);
        return key == null ? null : List.copyOf(lists.get(key));
    }

    /** Nombre tal como se guardó, o null. */
    public synchronized String realName(long user, String name)
    {
        Map<String, List<Entry>> lists = data.get(user);
        return lists == null ? null : findKey(lists, name);
    }

    /**
     * Guarda (o reemplaza) una playlist. Devuelve un mensaje de error o null
     * si salió bien.
     */
    public synchronized String save(long user, String name, List<Entry> entries)
    {
        Map<String, List<Entry>> lists = data.computeIfAbsent(user, u -> new LinkedHashMap<>());
        String key = findKey(lists, name);
        if(key == null && lists.size() >= MAX_PLAYLISTS)
            return "Ya tienes " + MAX_PLAYLISTS + " playlists; borra alguna antes de crear otra.";
        List<Entry> copy = new ArrayList<>(entries.subList(0, Math.min(entries.size(), MAX_TRACKS)));
        lists.put(key == null ? name : key, copy);
        write();
        return null;
    }

    /** Agrega canciones al final; crea la playlist si no existe. Devuelve cuántas entraron o -1 si no hay espacio. */
    public synchronized int append(long user, String name, List<Entry> entries)
    {
        Map<String, List<Entry>> lists = data.computeIfAbsent(user, u -> new LinkedHashMap<>());
        String key = findKey(lists, name);
        if(key == null)
        {
            if(lists.size() >= MAX_PLAYLISTS)
                return -1;
            key = name;
            lists.put(key, new ArrayList<>());
        }
        List<Entry> list = lists.get(key);
        int added = 0;
        for(Entry e : entries)
        {
            if(list.size() >= MAX_TRACKS)
                break;
            list.add(e);
            added++;
        }
        write();
        return added;
    }

    /** Quita la canción en la posición indicada (desde 1). */
    public synchronized Entry removeAt(long user, String name, int position)
    {
        Map<String, List<Entry>> lists = data.get(user);
        String key = lists == null ? null : findKey(lists, name);
        if(key == null)
            return null;
        List<Entry> list = lists.get(key);
        if(position < 1 || position > list.size())
            return null;
        Entry removed = list.remove(position - 1);
        write();
        return removed;
    }

    public synchronized boolean delete(long user, String name)
    {
        Map<String, List<Entry>> lists = data.get(user);
        String key = lists == null ? null : findKey(lists, name);
        if(key == null)
            return false;
        lists.remove(key);
        if(lists.isEmpty())
            data.remove(user);
        write();
        return true;
    }

    public synchronized List<String> search(long user, String input)
    {
        String in = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for(String n : names(user))
            if(in.isEmpty() || n.toLowerCase(Locale.ROOT).contains(in))
                out.add(n);
        return Collections.unmodifiableList(out);
    }

    private void write()
    {
        JSONObject root = new JSONObject();
        data.forEach((user, lists) -> {
            JSONObject obj = new JSONObject();
            lists.forEach((name, entries) -> {
                JSONArray arr = new JSONArray();
                for(Entry e : entries)
                    arr.put(new JSONObject().put("title", e.title()).put("uri", e.uri()));
                obj.put(name, arr);
            });
            root.put(Long.toString(user), obj);
        });
        try
        {
            // se escribe en un temporal y se mueve, así un corte de luz no deja el archivo a medias
            Path path = OtherUtil.getPath(FILE);
            Path tmp = path.resolveSibling(FILE + ".tmp");
            Files.writeString(tmp, root.toString(2), StandardCharsets.UTF_8);
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch(Exception e)
        {
            LOG.error("No pude guardar " + FILE, e);
        }
    }
}
