package org.drappula.arcadeCore.managers.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

class VoidChunkGeneratorTest {
    private final VoidChunkGenerator generator = new VoidChunkGenerator();
    private final World world = mock(World.class);

    /** A void world has no solid ground, so the server's spawn search would otherwise load chunk after chunk. */
    @Test
    void providesFixedSpawnSoTheServerNeverSearchesForOne() {
        Location spawn = generator.getFixedSpawnLocation(world, new Random());

        assertNotNull(spawn);
        assertEquals(world, spawn.getWorld());
    }

    @Test
    void everyColumnCountsAsSpawnable() {
        assertTrue(generator.canSpawn(world, 0, 0));
        assertTrue(generator.canSpawn(world, 1234, -987));
    }
}
