package org.drappula.arcadeApi.systems.map;

import org.bukkit.Location;
import org.jspecify.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface IMapManager {
    List<IArcadeMap> getMaps(String gameId);
    List<IArcadeMap> getAllMaps();
    List<IArcadeMap> getAvailableMaps(String gameId);
    @Nullable IArcadeMap getMap(String mapId);
    Optional<IArcadeMap> acquireMap(String gameId);
    void releaseMap(IArcadeMap map);

    void createMap(String mapId, String gameId, String displayName, String world) throws SQLException;
    void addSpawn(String mapId, Location location) throws SQLException;
    void setEnabled(String mapId, boolean enabled) throws SQLException;
    void deleteMap(String mapId) throws SQLException;
}
