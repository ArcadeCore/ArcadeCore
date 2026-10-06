package org.drappula.arcadeCore.config;

import org.bukkit.Location;
import org.drappula.arcadeCore.ServerTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataConfigTest extends ServerTest {

    @Test
    void spawnAbsentByDefault() {
        assertTrue(DataConfig.getSpawnLocation().isEmpty());
    }

    @Test
    void spawnRoundTrips() throws Exception {
        Location spawn = new Location(server.addSimpleWorld("world"), 10, 64, -5);

        DataConfig.setSpawnLocation(spawn);

        var loaded = DataConfig.getSpawnLocation();
        assertTrue(loaded.isPresent());
        assertEquals(10, loaded.get().getX());
        assertEquals(-5, loaded.get().getZ());
        assertEquals("world", loaded.get().getWorld().getName());
    }
}
