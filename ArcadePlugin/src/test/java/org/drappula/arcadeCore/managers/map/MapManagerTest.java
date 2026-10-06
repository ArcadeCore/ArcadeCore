package org.drappula.arcadeCore.managers.map;

import org.bukkit.Location;
import org.drappula.arcadeApi.systems.map.IArcadeMap;
import org.drappula.arcadeCore.ServerTest;
import org.drappula.arcadeCore.managers.game.GameManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MapManagerTest extends ServerTest {

    private static Location spawn(double x, double y, double z) {
        return new Location(null, x, y, z);
    }

    @Test
    void emptyPoolAcquiresNothing() {
        assertTrue(MapManager.get().acquireMap("game").isEmpty());
        assertTrue(MapManager.get().getMaps("game").isEmpty());
        assertTrue(MapManager.get().getAllMaps().isEmpty());
        assertNull(MapManager.get().getMap("missing"));
    }

    @Test
    void createAndLookupAreCaseInsensitive() throws Exception {
        MapManager.get().createMap("Arena", "Game", "Arena", "world");

        assertNotNull(MapManager.get().getMap("arena"));
        assertEquals(1, MapManager.get().getMaps("GAME").size());
        assertEquals(1, MapManager.get().getAllMaps().size());
        assertEquals("Arena", MapManager.get().getMap("arena").getDisplayName());
        assertEquals("world", MapManager.get().getMap("arena").getWorldName());
    }

    @Test
    void addedSpawnIsCloned() throws Exception {
        MapManager.get().createMap("arena", "game", "Arena", "world");
        Location location = spawn(1, 2, 3);

        MapManager.get().addSpawn("arena", location);
        location.setX(99);

        List<Location> stored = MapManager.get().getMap("arena").getSpawnPoints();
        assertEquals(1, stored.size());
        assertEquals(1, stored.get(0).getX());
    }

    @Test
    void addSpawnToUnknownMapIsNoop() {
        assertDoesNotThrow(() -> MapManager.get().addSpawn("missing", spawn(0, 0, 0)));
    }

    @Test
    void mapConfigSetPersists() throws Exception {
        MapManager.get().createMap("arena", "game", "Arena", "world");

        MapManager.get().setMapConfig("arena", "speed", "42");

        assertEquals("42", MapManager.get().getMap("arena").getConfigOverrides().get("speed"));
    }

    @Test
    void mapConfigDeleteReverts() throws Exception {
        MapManager.get().createMap("arena", "game", "Arena", "world");
        MapManager.get().setMapConfig("arena", "speed", "42");

        MapManager.get().deleteMapConfig("arena", "speed");

        assertTrue(MapManager.get().getMap("arena").getConfigOverrides().isEmpty());
    }

    private static class OptionGame {
        static org.drappula.arcadeApi.systems.game.Game game() {
            org.drappula.arcadeApi.systems.game.Game game =
                    org.mockito.Mockito.mock(org.drappula.arcadeApi.systems.game.Game.class);
            org.mockito.Mockito.when(game.getId()).thenReturn("game");
            org.mockito.Mockito.when(game.getMapConfigOptions()).thenReturn(java.util.List.of(
                    org.drappula.arcadeApi.systems.map.MapConfigOption.integer("speed", 5, 0, 60),
                    org.drappula.arcadeApi.systems.map.MapConfigOption.text("motd", "hello")));
            return game;
        }
    }

    @Test
    void createMapPrefillsRegisteredDefaults() throws Exception {
        GameManager.get().registerGame(OptionGame.game());

        MapManager.get().createMap("arena", "game", "Arena", "world");

        assertEquals(java.util.Map.of("speed", "5", "motd", "hello"),
                MapManager.get().getMap("arena").getConfigOverrides());
    }

    @Test
    void createMapWithoutRegisteredGamePrefillsNothing() throws Exception {
        MapManager.get().createMap("arena", "game", "Arena", "world");

        assertTrue(MapManager.get().getMap("arena").getConfigOverrides().isEmpty());
    }

    @Test
    void setMapConfigAfterPrefillOverwrites() throws Exception {
        GameManager.get().registerGame(OptionGame.game());
        MapManager.get().createMap("arena", "game", "Arena", "world");

        MapManager.get().setMapConfig("arena", "speed", "42");

        assertEquals("42", MapManager.get().getMap("arena").getConfigOverrides().get("speed"));
    }

    @Test
    void onlyEnabledFreeSpawnedMapsAreAvailable() throws Exception {
        MapManager.get().createMap("ready", "game", "Ready", "world");
        MapManager.get().addSpawn("ready", spawn(0, 64, 0));
        MapManager.get().createMap("empty", "game", "Empty", "world");
        MapManager.get().createMap("off", "game", "Off", "world");
        MapManager.get().addSpawn("off", spawn(0, 64, 0));
        MapManager.get().setEnabled("off", false);

        List<IArcadeMap> available = MapManager.get().getAvailableMaps("game");

        assertEquals(1, available.size());
        assertEquals("ready", available.get(0).getId());
    }

    @Test
    void acquireReservesAndReleaseFrees() throws Exception {
        MapManager.get().createMap("arena", "game", "Arena", "world");
        MapManager.get().addSpawn("arena", spawn(0, 64, 0));

        Optional<IArcadeMap> acquired = MapManager.get().acquireMap("game");

        assertTrue(acquired.isPresent());
        assertTrue(acquired.get().isInUse());
        assertTrue(MapManager.get().acquireMap("game").isEmpty());

        MapManager.get().releaseMap(acquired.get());
        assertFalse(MapManager.get().getMap("arena").isInUse());
        assertTrue(MapManager.get().acquireMap("game").isPresent());
    }

    @Test
    void releaseNullOrUntrackedIsNoop() {
        assertDoesNotThrow(() -> {
            MapManager.get().releaseMap(null);
            MapManager.get().releaseMap(new ArcadeMap("ghost", "game", "Ghost", "world", true, false, new ArrayList<>()));
        });
    }

    @Test
    void deleteRemovesMap() throws Exception {
        MapManager.get().createMap("arena", "game", "Arena", "world");

        MapManager.get().deleteMap("ARENA");

        assertNull(MapManager.get().getMap("arena"));
        assertTrue(MapManager.get().getAllMaps().isEmpty());
    }

    @Test
    void releaseAllClearsInUseFlags() throws Exception {
        MapManager.get().createMap("arena", "game", "Arena", "world");
        MapManager.get().addSpawn("arena", spawn(0, 64, 0));
        MapManager.get().acquireMap("game");

        MapManager.get().releaseAll();

        assertFalse(MapManager.get().getMap("arena").isInUse());
    }

    @Test
    void databaseFailureDuringAcquireReleaseLogsAndContinues() throws Exception {
        MapManager.get().createMap("arena", "game", "Arena", "world");
        MapManager.get().addSpawn("arena", spawn(0, 64, 0));
        java.lang.reflect.Field field =
                org.drappula.arcadeCore.database.Database.class.getDeclaredField("connection");
        field.setAccessible(true);
        ((java.sql.Connection) field.get(null)).close();
        try {
            // Persistence fails, but in-memory flags still flip and callers proceed.
            assertTrue(MapManager.get().acquireMap("game").isPresent());
            assertTrue(MapManager.get().getMap("arena").isInUse());

            MapManager.get().releaseMap(MapManager.get().getMap("arena"));
            assertFalse(MapManager.get().getMap("arena").isInUse());

            MapManager.get().acquireMap("game");
            MapManager.get().releaseAll();
            assertFalse(MapManager.get().getMap("arena").isInUse());

            MapManager.get().load();
            assertTrue(MapManager.get().getAllMaps().isEmpty());
        } finally {
            org.drappula.arcadeCore.database.TestDb.disconnect();
            org.drappula.arcadeCore.database.TestDb.connect();
        }
    }

    @Test
    void loadPullsPersistedMaps() throws Exception {
        server.addSimpleWorld("world");
        // Seed the database directly, bypassing the in-memory list.
        org.drappula.arcadeCore.database.MapDataManager.create("arena", "game", "Arena", "world");
        org.drappula.arcadeCore.database.MapDataManager.addSpawn("arena", 0, spawn(0, 64, 0));
        assertTrue(MapManager.get().getAllMaps().isEmpty());

        MapManager.get().load();

        assertEquals(1, MapManager.get().getAllMaps().size());
        assertEquals(1, MapManager.get().getMap("arena").getSpawnPoints().size());
    }

    @Test
    void worldResolvesWhenLoaded() throws Exception {
        server.addSimpleWorld("world");
        MapManager.get().createMap("arena", "game", "Arena", "world");

        assertNotNull(MapManager.get().getMap("arena").getWorld());
        assertEquals("world", MapManager.get().getMap("arena").getWorld().getName());
    }

    @Test
    void worldIsNullWhenNotLoaded() throws Exception {
        MapManager.get().createMap("arena", "game", "Arena", "missing");

        assertNull(MapManager.get().getMap("arena").getWorld());
    }
}
