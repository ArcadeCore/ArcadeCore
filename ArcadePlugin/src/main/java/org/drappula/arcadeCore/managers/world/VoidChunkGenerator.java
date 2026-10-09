package org.drappula.arcadeCore.managers.world;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

/**
 * Generates nothing: arena worlds start completely empty, games build into them.
 * Uses the chunk-data API that exists from 1.8 on; newer servers still accept it.
 */
@SuppressWarnings("deprecation")
public class VoidChunkGenerator extends ChunkGenerator {

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        return createChunkData(world);
    }

    /**
     * A void world has no ground, so without a fixed spawn the server searches for one by loading chunk after
     * chunk, which on 1.8 and 1.12 froze the main thread past the watchdog. Games teleport players to their own
     * spawn points, so the world spawn only has to exist.
     */
    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5, 64, 0.5);
    }

    @Override
    public boolean canSpawn(World world, int x, int z) {
        return true;
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return Collections.emptyList();
    }
}
