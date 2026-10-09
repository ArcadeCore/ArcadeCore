package org.drappula.arcadeCore.managers.game;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IParticipant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Glass spawn cages built around each participant after teleport, restored
 * (not just cleared) when the match starts or ends mid-countdown — cage
 * cells on hand-built maps may have held real blocks.
 */
public final class SpawnCages {
    private SpawnCages() {
    }

    /** Builds cages for every participant, recording the snapshot on the match. */
    public static void buildAll(Match match) {
        Map<Location, BlockState> snapshot = new LinkedHashMap<Location, BlockState>();
        for (IParticipant participant : match.getParticipants()) {
            Player player = participant.getPlayer();
            if (player == null) continue;
            snapshot.putAll(build(player.getLocation()));
        }
        match.setCageSnapshotInternal(snapshot);
    }

    /** Restores caged cells to their pre-build types. Idempotent. */
    public static void clear(IMatch match) {
        if (!(match instanceof Match)) return;
        Match concrete = (Match) match;
        Map<Location, BlockState> snapshot = concrete.getCageSnapshotInternal();
        concrete.setCageSnapshotInternal(null);
        if (snapshot == null) return;
        for (Map.Entry<Location, BlockState> cell : snapshot.entrySet()) {
            if (cell.getKey().getWorld() == null) continue;
            // BlockState keeps the data value (1.8 to 1.12) and the block state (1.13+), not just the material.
            cell.getValue().update(true, false);
        }
    }

    /** 3x3 ring around the feet block at two levels plus the cap. Feet stay free. */
    static Map<Location, BlockState> build(Location feet) {
        Map<Location, BlockState> snapshot = new LinkedHashMap<Location, BlockState>();
        World world = feet.getWorld();
        int x = feet.getBlockX();
        int y = feet.getBlockY();
        int z = feet.getBlockZ();
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    cageCell(snapshot, world, x + dx, y + dy, z + dz);
                }
            }
        }
        cageCell(snapshot, world, x, y + 2, z);
        return snapshot;
    }

    private static void cageCell(Map<Location, BlockState> snapshot, World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        snapshot.putIfAbsent(block.getLocation(), block.getState());
        block.setType(Material.GLASS);
    }
}
