package com.itstuki.tukibot.update;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UpdaterTest
{
    @Test
    void comparesVersions()
    {
        assertTrue(Updater.compare("v1.3.0", "1.2.1") > 0);
        assertTrue(Updater.compare("v1.2.10", "1.2.9") > 0);
        assertEquals(0, Updater.compare("v1.3.0", "1.3.0"));
        assertTrue(Updater.compare("1.2", "1.2.1") < 0);
        assertTrue(Updater.compare("v1.3.0-beta", "1.2.9") > 0);
    }
}
