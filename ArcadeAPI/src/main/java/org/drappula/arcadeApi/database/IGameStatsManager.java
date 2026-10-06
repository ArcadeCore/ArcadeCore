package org.drappula.arcadeApi.database;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IGameStatsManager {
    Optional<GameStats> getStats(UUID uuid, String gameId) throws SQLException;
    List<GameStats> getTopPlayers(String gameId, int limit) throws SQLException;
    void addPoints(UUID uuid, String username, String gameId, int delta) throws SQLException;
}
