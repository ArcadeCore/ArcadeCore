package org.drappula.arcadeCore.managers.map;

import org.drappula.arcadeCore.util.Immutable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.drappula.arcadeApi.systems.map.IArcadeMap;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArcadeMap implements IArcadeMap {
    private final String id;
    private final String gameId;
    private final String displayName;
    private final String worldName;
    private boolean enabled;
    private boolean inUse;
    private final List<Location> spawnPoints;
    private final Map<String, String> config = new HashMap<>();

    public ArcadeMap(String id, String gameId, String displayName, String worldName, boolean enabled, boolean inUse, List<Location> spawnPoints) {
        this.id = id;
        this.gameId = gameId;
        this.displayName = displayName;
        this.worldName = worldName;
        this.enabled = enabled;
        this.inUse = inUse;
        this.spawnPoints = spawnPoints;
    }

    public String getId() {
        return id;
    }
    public String getGameId() {
        return gameId;
    }
    public String getDisplayName() {
        return displayName;
    }
    public String getWorldName() {
        return worldName;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public boolean isInUse() {
        return inUse;
    }
    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
    public List<Location> getSpawnPoints() {
        return Immutable.copy(spawnPoints);
    }

    /** Internal mutable view for spawn administration. */
    List<Location> getSpawnPointsInternal() {
        return spawnPoints;
    }

    /** Internal mutable view for config administration (database load, manager write-through). */
    public Map<String, String> getConfigInternal() {
        return config;
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return java.util.Collections.unmodifiableMap(new java.util.HashMap<>(config));
    }

    @Override
    public @Nullable World getWorld() {
        return Bukkit.getWorld(worldName);
    }
}
