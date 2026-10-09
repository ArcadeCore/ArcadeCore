package org.drappula.arcadeCore.managers.map;

import org.drappula.arcadeCore.util.Immutable;
import org.drappula.arcadeCore.util.Log;
import org.bukkit.Location;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.map.IArcadeMap;
import org.drappula.arcadeApi.systems.map.IMapManager;
import org.drappula.arcadeApi.systems.map.MapConfigOption;
import org.drappula.arcadeCore.ArcadeCore;
import org.drappula.arcadeCore.database.MapDataManager;
import org.drappula.arcadeCore.managers.game.GameManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MapManager implements IMapManager {
    private static MapManager instance;
    public static MapManager get() {
        if (instance == null) instance = new MapManager();
        return instance;
    }

    private final List<ArcadeMap> maps = new ArrayList<>();

    public void load() {
        maps.clear();
        try {
            maps.addAll(MapDataManager.loadAll());
        } catch (SQLException e) {
            Log.error("Failed to load maps from the database", e);
        }
    }

    public List<IArcadeMap> getMaps(String gameId) {
        List<IArcadeMap> result = new ArrayList<>();
        for (ArcadeMap map : maps) {
            if (map.getGameId().equalsIgnoreCase(gameId)) result.add(map);
        }
        return Immutable.copy(result);
    }

    @Override
    public List<IArcadeMap> getAllMaps() {
        return Immutable.copy(new ArrayList<IArcadeMap>(maps));
    }

    @Override
    public List<IArcadeMap> getAvailableMaps(String gameId) {
        List<IArcadeMap> result = new ArrayList<>();
        for (ArcadeMap map : maps) {
            if (map.getGameId().equalsIgnoreCase(gameId) && map.isAvailable()) result.add(map);
        }
        return Immutable.copy(result);
    }

    public ArcadeMap getMap(String mapId) {
        for (ArcadeMap map : maps) {
            if (map.getId().equalsIgnoreCase(mapId)) return map;
        }
        return null;
    }

    public Optional<IArcadeMap> acquireMap(String gameId) {
        for (ArcadeMap map : maps) {
            if (map.getGameId().equalsIgnoreCase(gameId) && map.isEnabled() && !map.isInUse() && !map.getSpawnPointsInternal().isEmpty()) {
                map.setInUse(true);
                try {
                    MapDataManager.setInUse(map.getId(), true);
                } catch (SQLException e) {
                    Log.error("Failed to persist in-use flag for map {}", map.getId(), e);
                }
                return Optional.of(map);
            }
        }
        return Optional.empty();
    }

    public void releaseMap(IArcadeMap map) {
        if (map == null) return;
        ArcadeMap tracked = getMap(map.getId());
        if (tracked == null) return;
        tracked.setInUse(false);
        try {
            MapDataManager.setInUse(tracked.getId(), false);
        } catch (SQLException e) {
            Log.error("Failed to persist in-use flag for map {}", tracked.getId(), e);
        }
    }

    public void releaseAll() {
        for (ArcadeMap map : maps) map.setInUse(false);
        try {
            MapDataManager.releaseAll();
        } catch (SQLException e) {
            Log.error("Failed to release maps in the database", e);
        }
    }

    public void createMap(String mapId, String gameId, String displayName, String world) throws SQLException {
        MapDataManager.create(mapId, gameId, displayName, world);
        maps.add(new ArcadeMap(mapId, gameId, displayName, world, true, false, new ArrayList<>()));
        ensureDefaultConfig(mapId);
    }

    /**
     * Ensures defaults for every map of a game (e.g. options the addon added
     * since the maps were created). Never overwrites stored values.
     */
    public void backfillDefaultConfig(String gameId) {
        for (ArcadeMap map : maps) {
            if (!map.getGameId().equalsIgnoreCase(gameId)) continue;
            try {
                ensureDefaultConfig(map.getId());
            } catch (SQLException e) {
                Log.error("Failed to backfill config for map {}", map.getId(), e);
            }
        }
    }

    /**
     * Writes every registered default for the map's game that has no stored
     * value yet. Never overwrites admin customization. No-op when the map or
     * its game is unknown.
     */
    public void ensureDefaultConfig(String mapId) throws SQLException {
        ArcadeMap map = getMap(mapId);
        if (map == null) return;
        Game game = GameManager.get().getGame(map.getGameId());
        if (game == null) return;
        for (MapConfigOption option : game.getMapConfigOptions()) {
            MapDataManager.ensureConfig(mapId, option.key(), option.defaultValue());
            map.getConfigInternal().putIfAbsent(option.key(), option.defaultValue());
        }
    }

    public void addSpawn(String mapId, Location location) throws SQLException {
        ArcadeMap map = getMap(mapId);
        if (map == null) return;
        MapDataManager.addSpawn(mapId, map.getSpawnPointsInternal().size(), location);
        map.getSpawnPointsInternal().add(location.clone());
    }

    public void setEnabled(String mapId, boolean enabled) throws SQLException {
        ArcadeMap map = getMap(mapId);
        if (map == null) return;
        MapDataManager.setEnabled(mapId, enabled);
        map.setEnabled(enabled);
    }

    public void deleteMap(String mapId) throws SQLException {
        MapDataManager.delete(mapId);
        maps.removeIf(m -> m.getId().equalsIgnoreCase(mapId));
    }

    public void setMapConfig(String mapId, String key, String value) throws SQLException {
        ArcadeMap map = getMap(mapId);
        if (map == null) return;
        MapDataManager.setConfig(mapId, key, value);
        map.getConfigInternal().put(key, value);
    }

    public void deleteMapConfig(String mapId, String key) throws SQLException {
        ArcadeMap map = getMap(mapId);
        if (map == null) return;
        MapDataManager.deleteConfig(mapId, key);
        map.getConfigInternal().remove(key);
    }
}
