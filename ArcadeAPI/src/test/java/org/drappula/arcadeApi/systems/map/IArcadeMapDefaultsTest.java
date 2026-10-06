package org.drappula.arcadeApi.systems.map;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class IArcadeMapDefaultsTest {

    private static IArcadeMap map(boolean enabled, boolean inUse, List<Location> spawns) {
        return new IArcadeMap() {
            @Override
            public String getId() {
                return "map";
            }

            @Override
            public String getGameId() {
                return "game";
            }

            @Override
            public String getDisplayName() {
                return "Map";
            }

            @Override
            public String getWorldName() {
                return "world";
            }

            @Override
            public boolean isEnabled() {
                return enabled;
            }

            @Override
            public boolean isInUse() {
                return inUse;
            }

            @Override
            public List<Location> getSpawnPoints() {
                return spawns;
            }

            @Override
            public org.bukkit.World getWorld() {
                return null;
            }
        };
    }

    @Test
    void spawnCountMirrorsSpawnPoints() {
        assertEquals(3, map(true, false, List.of(mock(Location.class), mock(Location.class), mock(Location.class))).getSpawnCount());
        assertEquals(0, map(true, false, List.of()).getSpawnCount());
    }

    @Test
    void availableOnlyWhenEnabledFreeAndSpawned() {
        Location spawn = mock(Location.class);

        assertTrue(map(true, false, List.of(spawn)).isAvailable());
        assertFalse(map(false, false, List.of(spawn)).isAvailable());
        assertFalse(map(true, true, List.of(spawn)).isAvailable());
        assertFalse(map(true, false, List.of()).isAvailable());
    }

    @Test
    void randomSpawnNullWithoutSpawns() {
        assertNull(map(true, false, List.of()).getRandomSpawn());
    }

    @Test
    void randomSpawnComesFromSpawnPoints() {
        Location a = mock(Location.class);
        Location b = mock(Location.class);
        IArcadeMap arcadeMap = map(true, false, List.of(a, b));

        for (int i = 0; i < 20; i++) {
            assertTrue(arcadeMap.getSpawnPoints().contains(arcadeMap.getRandomSpawn()));
        }
    }
}
