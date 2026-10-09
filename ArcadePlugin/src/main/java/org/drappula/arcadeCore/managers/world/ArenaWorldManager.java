package org.drappula.arcadeCore.managers.world;

import org.drappula.arcadeCore.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.MatchState;
import org.drappula.arcadeApi.systems.map.IArcadeMap;
import org.drappula.arcadeCore.ArcadeCore;
import org.drappula.arcadeCore.managers.game.MatchManager;
import org.drappula.arcadeCore.util.PlayerUtil;
import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Provisions one throwaway void world per match. Games build their arenas inside it
 * (see {@code Game.createArena}) so play worlds — and their borders — are never touched.
 * Worlds are unloaded and deleted from disk when the match ends; {@link #destroyAll()},
 * {@link #sweepIdleArenas()} and {@link #destroyOrphanedArenas()} cover the paths where
 * normal teardown never ran (shutdown, crashes, players wandering out mid-match).
 */
public class ArenaWorldManager {
    private static ArenaWorldManager instance;
    public static ArenaWorldManager get() {
        if (instance == null) instance = new ArenaWorldManager();
        return instance;
    }

    private static final String PREFIX = "arcade-";
    private static final Pattern ORPHAN_NAME = Pattern.compile("^arcade-.*-[0-9a-f]{8}$");

    private final Set<UUID> arenaWorlds = new HashSet<>();

    public World create(String gameId) {
        String safe = gameId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        String name = PREFIX + safe + "-" + UUID.randomUUID().toString().substring(0, 8);
        WorldCreator creator = new WorldCreator(name)
                .environment(World.Environment.NORMAL)
                .generateStructures(false)
                .generator(new VoidChunkGenerator());
        World world = Bukkit.createWorld(creator);
        if (world == null) {
            throw new IllegalStateException("Could not create arena world " + name);
        }
        world.setAutoSave(false);
        arenaWorlds.add(world.getUID());
        return world;
    }

    public boolean isArenaWorld(@Nullable World world) {
        return world != null && arenaWorlds.contains(world.getUID());
    }

    /** Unloads and deletes a managed arena world. Foreign worlds are ignored. Never throws. */
    public void destroy(@Nullable World world) {
        if (world == null || !arenaWorlds.remove(world.getUID())) return;
        unloadAndDelete(world);
    }

    /** Evacuates players to the lobby, then destroys every managed arena world. */
    public void destroyAll() {
        for (World world : new ArrayList<>(listTracked())) {
            evacuate(world);
            destroy(world);
        }
    }

    /**
     * Destroys tracked worlds that sit empty with no live match bound to them
     * (everyone wandered or logged out mid-match). Runs periodically; see onEnable.
     */
    public void sweepIdleArenas() {
        for (World world : new ArrayList<>(listTracked())) {
            if (!world.getPlayers().isEmpty()) continue;
            if (hasLiveMatch(world)) continue;
            destroy(world);
        }
    }

    /**
     * Cleans up after crashes: unloads tracked-name worlds that were never registered
     * (previous session died) and deletes dead arena folders left on disk.
     */
    public void destroyOrphanedArenas() {
        for (World world : new ArrayList<>(Bukkit.getWorlds())) {
            if (world.getName().startsWith(PREFIX) && !arenaWorlds.contains(world.getUID())) {
                evacuate(world);
                unloadAndDelete(world);
            }
        }
        File[] dirs = Bukkit.getWorldContainer().listFiles(File::isDirectory);
        if (dirs == null) return;
        for (File dir : dirs) {
            if (ORPHAN_NAME.matcher(dir.getName()).matches() && Bukkit.getWorld(dir.getName()) == null) {
                deleteFolder(dir, dir.getName());
            }
        }
    }

    private boolean hasLiveMatch(World world) {
        for (IMatch match : MatchManager.get().getAllMatches()) {
            IArcadeMap map = match.getMap();
            if (map != null && world.equals(map.getWorld()) && match.getState() != MatchState.ENDED) return true;
        }
        return false;
    }

    private void evacuate(World world) {
        for (Player player : new ArrayList<>(world.getPlayers())) {
            PlayerUtil.sendToLobby(player);
        }
    }

    private ArrayList<World> listTracked() {
        ArrayList<World> worlds = new ArrayList<>();
        for (UUID uid : arenaWorlds) {
            World world = Bukkit.getWorld(uid);
            if (world != null) worlds.add(world);
        }
        return worlds;
    }

    private void unloadAndDelete(World world) {
        String name = world.getName();
        File folder = world.getWorldFolder();
        boolean unloaded;
        try {
            unloaded = Bukkit.unloadWorld(world, false);
        } catch (RuntimeException e) {
            Log.warn("Could not unload arena world {}", name, e);
            return;
        }
        if (!unloaded) {
            Log.warn("Could not unload arena world {}", name);
            return;
        }
        deleteFolder(folder, name);
    }

    private void deleteFolder(File folder, String name) {
        if (!folder.exists()) return;
        try (Stream<java.nio.file.Path> walk = Files.walk(folder.toPath())) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            Log.warn("Could not delete arena world folder {}", folder, e);
        }
    }
}
