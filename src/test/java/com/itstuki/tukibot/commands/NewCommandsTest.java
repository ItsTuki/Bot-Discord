package com.itstuki.tukibot.commands;

import static org.junit.jupiter.api.Assertions.*;

import com.itstuki.tukibot.commands.admin.ConfigCmd;
import com.itstuki.tukibot.commands.dj.AutoplayCmd;
import com.itstuki.tukibot.commands.dj.FilterCmd;
import com.itstuki.tukibot.commands.music.MyPlaylistCmd;
import java.util.List;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.junit.jupiter.api.Test;

/** Arma los comandos nuevos como lo haría Discord: JDA valida largos, nombres y opciones. */
class NewCommandsTest
{
    @Test
    void buildValidCommandData()
    {
        for(SlashCommand cmd : List.of(new AutoplayCmd(null), new FilterCmd(null), new MyPlaylistCmd(null), new ConfigCmd(null)))
        {
            SlashCommandData data = cmd.build();
            String json = data.toData().toString();
            assertTrue(json.contains(cmd.getName()));
            assertTrue(json.length() < 8000, cmd.getName() + " supera el tamaño máximo");
        }
    }
}
