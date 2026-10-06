package org.drappula.arcadeCore.database;

import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapDataManagerTest {

    @BeforeEach
    void openDb() throws Exception {
        TestDb.connect();
    }

    @AfterEach
    void closeDb() throws Exception {
        TestDb.disconnect();
    }

    @Test
    void spawnCountTracksInserts() throws Exception {
        MapDataManager.create("arena", "game", "Arena", "world");

        assertEquals(0, MapDataManager.countSpawns("arena"));

        MapDataManager.addSpawn("arena", 0, new Location(null, 0, 64, 0));
        MapDataManager.addSpawn("arena", 1, new Location(null, 10, 64, 10));

        assertEquals(2, MapDataManager.countSpawns("arena"));
        assertEquals(0, MapDataManager.countSpawns("missing"));
    }

    @Test
    void deletingMapCascadesToSpawns() throws Exception {
        MapDataManager.create("arena", "game", "Arena", "world");
        MapDataManager.addSpawn("arena", 0, new Location(null, 0, 64, 0));

        MapDataManager.delete("arena");

        assertEquals(0, MapDataManager.countSpawns("arena"));
    }

    @Test
    void configRoundTrips() throws Exception {
        MapDataManager.create("arena", "game", "Arena", "world");

        MapDataManager.setConfig("arena", "speed", "42");

        assertEquals(java.util.Map.of("speed", "42"), MapDataManager.loadConfigs().get("arena"));
    }

    @Test
    void configSetOverwrites() throws Exception {
        MapDataManager.create("arena", "game", "Arena", "world");
        MapDataManager.setConfig("arena", "speed", "1");

        MapDataManager.setConfig("arena", "speed", "42");

        assertEquals(java.util.Map.of("speed", "42"), MapDataManager.loadConfigs().get("arena"));
    }

    @Test
    void deletingMapCascadesToConfig() throws Exception {
        MapDataManager.create("arena", "game", "Arena", "world");
        MapDataManager.setConfig("arena", "speed", "42");

        MapDataManager.delete("arena");

        assertNull(MapDataManager.loadConfigs().get("arena"));
    }

    @Test
    void ensureDoesNotOverwrite() throws Exception {
        MapDataManager.create("arena", "game", "Arena", "world");
        MapDataManager.setConfig("arena", "speed", "42");

        MapDataManager.ensureConfig("arena", "speed", "5");
        MapDataManager.ensureConfig("arena", "motd", "hi");

        assertEquals(java.util.Map.of("speed", "42", "motd", "hi"), MapDataManager.loadConfigs().get("arena"));
    }
}
