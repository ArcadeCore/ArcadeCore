package org.drappula.arcadeCore.managers.world;

import java.util.Collections;
import java.util.List;
import java.util.Random;
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

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return Collections.emptyList();
    }
}
