package org.drappula.arcadeApi.database;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IGameStatsManager {
    Optional<GameStats> getStats(UUID uuid, String gameId) throws SQLException;
    List<GameStats> getTopPlayers(String gameId, int limit) throws SQLException;
    void addPoints(UUID uuid, String username, String gameId, int delta) throws SQLException;

    /** Adds {@code delta} to a custom per-game counter (e.g. "kills"). Key must match {@code [a-z0-9_]+}. */
    void addStat(UUID uuid, String username, String gameId, String key, int delta) throws SQLException;

    /** Custom counter value, 0 if absent. */
    int getStat(UUID uuid, String gameId, String key) throws SQLException;

    /** Players ranked by total wins then points across all games; each entry has gameId {@code "*"}. */
    List<GameStats> getTopOverall(int limit) throws SQLException;
}
