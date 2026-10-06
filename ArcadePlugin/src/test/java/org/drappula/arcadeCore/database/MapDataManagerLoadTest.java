package org.drappula.arcadeCore.database;

import org.bukkit.Location;
import org.drappula.arcadeCore.ServerTest;
import org.drappula.arcadeCore.managers.map.ArcadeMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapDataManagerLoadTest extends ServerTest {

    @Test
    void loadsMapsWithSpawnsForLoadedWorlds() throws Exception {
        server.addSimpleWorld("world");
        MapDataManager.create("arena", "game", "Arena", "world");
        MapDataManager.addSpawn("arena", 0, new Location(null, 1, 64, 2));
        MapDataManager.addSpawn("arena", 1, new Location(null, 3, 64, 4));

        List<ArcadeMap> maps = MapDataManager.loadAll();

        assertEquals(1, maps.size());
        assertEquals("arena", maps.get(0).getId());
        assertEquals(2, maps.get(0).getSpawnPoints().size());
        assertEquals(1, maps.get(0).getSpawnPoints().get(0).getX());
        assertEquals("world", maps.get(0).getSpawnPoints().get(0).getWorld().getName());
    }

    @Test
    void skipsMapsWhoseWorldIsNotLoaded() throws Exception {        server.addSimpleWorld("world");
        MapDataManager.create("ready", "game", "Ready", "world");
        MapDataManager.addSpawn("ready", 0, new Location(null, 0, 64, 0));
        MapDataManager.create("void", "game", "Void", "missing");
        MapDataManager.addSpawn("void", 0, new Location(null, 0, 64, 0));

        List<ArcadeMap> maps = MapDataManager.loadAll();

        assertEquals(1, maps.size());
        assertEquals("ready", maps.get(0).getId());
    }

    @Test
    void loadAllRestoresConfig() throws Exception {
        server.addSimpleWorld("world");
        MapDataManager.create("arena", "game", "Arena", "world");
        MapDataManager.setConfig("arena", "speed", "42");

        List<ArcadeMap> maps = MapDataManager.loadAll();

        assertEquals(1, maps.size());
        assertEquals("42", maps.get(0).getConfigOverrides().get("speed"));
    }
}
