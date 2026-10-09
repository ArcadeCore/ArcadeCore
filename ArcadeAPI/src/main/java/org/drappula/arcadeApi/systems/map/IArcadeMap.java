package org.drappula.arcadeApi.systems.map;

import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.Nullable;

import org.drappula.arcadeApi.ArcadeAPIProvider;
import org.drappula.arcadeApi.systems.game.Game;

import java.util.List;
import java.util.Map;

public interface IArcadeMap {
    String getId();
    String getGameId();
    String getDisplayName();
    String getWorldName();
    boolean isEnabled();
    boolean isInUse();
    List<Location> getSpawnPoints();

    /** Null when the world isn't loaded. */
    @Nullable World getWorld();

    default int getSpawnCount() {
        return getSpawnPoints().size();
    }

    default boolean isAvailable() {
        return isEnabled() && !isInUse() && !getSpawnPoints().isEmpty();
    }

    /** Raw per-map overrides set via {@code /arcade map config}; empty by default. */
    default Map<String, String> getConfigOverrides() {
        return java.util.Collections.emptyMap();
    }

    /**
     * Effective value for a registered option: the per-map override when
     * present, otherwise the option default. Unknown keys throw — callers
     * should only ask for options their game registered. Overrides that no
     * longer validate (hand-edited database) warn and fall back to default.
     */
    default String getConfigValue(String key) {
        MapConfigOption option = requireConfigOption(key);
        String override = getConfigOverrides().get(key);
        if (override == null) return option.defaultValue();
        try {
            return option.validateAndCanonicalize(override);
        } catch (IllegalArgumentException e) {
            java.util.logging.Logger.getLogger(IArcadeMap.class.getName()).warning(
                    "Ignoring invalid stored value for map " + getId() + " option " + key + ": " + e.getMessage());
            return option.defaultValue();
        }
    }

    default int getIntConfig(String key) {
        return Integer.parseInt(getConfigValue(key));
    }

    default double getDecimalConfig(String key) {
        return Double.parseDouble(getConfigValue(key));
    }

    default boolean getBooleanConfig(String key) {
        return Boolean.parseBoolean(getConfigValue(key));
    }

    default String getTextConfig(String key) {
        return getConfigValue(key);
    }

    // Java 8 has no private interface methods, so this helper is public. Addons should use getConfigValue instead.
    default MapConfigOption requireConfigOption(String key) {
        Game game = ArcadeAPIProvider.get().getGameManager().getGame(getGameId());
        if (game != null) {
            for (MapConfigOption option : game.getMapConfigOptions()) {
                if (option.key().equals(key)) return option;
            }
        }
        throw new IllegalArgumentException("Unknown map config option '" + key + "' for game '" + getGameId() + "'");
    }

    /** Null when the map has no spawns. */
    default @Nullable Location getRandomSpawn() {
        List<Location> spawns = getSpawnPoints();
        if (spawns.isEmpty()) return null;
        return spawns.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(spawns.size()));
    }
}
