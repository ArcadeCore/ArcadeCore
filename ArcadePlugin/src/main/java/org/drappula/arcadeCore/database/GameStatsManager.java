package org.drappula.arcadeCore.database;

import org.drappula.arcadeCore.util.Immutable;
import java.util.Collections;
import org.drappula.arcadeCore.util.Log;
import org.drappula.arcadeApi.database.GameStats;
import org.drappula.arcadeApi.database.IGameStatsManager;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.drappula.arcadeCore.ArcadeCore;
import org.drappula.arcadeCore.managers.UserDataManager;
import org.drappula.arcadeCore.managers.game.Match;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Framework-side recording of match outcomes into {@code games}/{@code game_stats}.
 * Declaring winners stays addon-side ({@link Match#setWinnerParticipants}); this only persists.
 * If the addon declared no winners the outcome is unknown and nothing is recorded.
 */
public class GameStatsManager implements IGameStatsManager {
    private static final GameStatsManager INSTANCE = new GameStatsManager();

    public static GameStatsManager get() {
        return INSTANCE;
    }

    public static void recordMatchResult(IMatch match) {
        get().recordResult(match);
    }

    private void recordResult(IMatch match) {
        try {
            String gameId = match.getGame().getId();
            try (PreparedStatement stmt = Database.get().prepareStatement(
                    "INSERT OR IGNORE INTO games (game_id) VALUES (?)")) {
                stmt.setString(1, gameId);
                stmt.executeUpdate();
            }

            Set<UUID> winnerIds = new HashSet<>();
            for (IParticipant winner : match.getWinnerParticipants()) {
                winnerIds.add(winner.getPlayer().getUniqueId());
            }
            if (winnerIds.isEmpty()) return;

            // Only actual participants earn wins/losses. SpectatingPlayers mirrors
            // eliminated participants today, but must never grant stats to pure
            // spectators if addons use it for viewers later.
            Map<UUID, String> players = new LinkedHashMap<>();
            for (IParticipant participant : match.getParticipants()) {
                players.put(participant.getPlayer().getUniqueId(), participant.getPlayer().getName());
            }
            for (IParticipant participant : match.getEliminatedParticipants()) {
                players.putIfAbsent(participant.getPlayer().getUniqueId(), participant.getPlayer().getName());
            }

            for (Map.Entry<UUID, String> entry : players.entrySet()) {
                // game_stats references user_profiles; ensure the row exists first.
                UserDataManager.getOrCreate(entry.getKey(), entry.getValue());
                boolean won = winnerIds.contains(entry.getKey());
                ensureRow(entry.getKey(), gameId);
                try (PreparedStatement stmt = Database.get().prepareStatement(
                        "UPDATE game_stats SET wins = wins + ?, losses = losses + ? WHERE uuid = ? AND game_id = ?")) {
                    stmt.setInt(1, won ? 1 : 0);
                    stmt.setInt(2, won ? 0 : 1);
                    stmt.setString(3, entry.getKey().toString());
                    stmt.setString(4, gameId);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            Log.error("Failed to record match result for game {}", match.getGame().getId(), e);
        }
    }

    @Override
    public Optional<GameStats> getStats(UUID uuid, String gameId) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "SELECT points, wins, losses, other_stats FROM game_stats WHERE uuid = ? AND game_id = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, gameId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new GameStats(uuid, gameId,
                        rs.getInt("points"), rs.getInt("wins"), rs.getInt("losses"), parseOther(rs.getString("other_stats"))));
            }
        }
    }

    private static Map<String, Integer> parseOther(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyMap();
        try {
            Map<String, Integer> out = new LinkedHashMap<>();
            com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(json).getAsJsonObject();
            obj.entrySet().forEach(e -> out.put(e.getKey(), e.getValue().getAsInt()));
            return out;
        } catch (RuntimeException e) {
            return Collections.emptyMap();
        }
    }

    @Override
    public List<GameStats> getTopPlayers(String gameId, int limit) throws SQLException {
        List<GameStats> top = new ArrayList<>();
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "SELECT uuid, points, wins, losses, other_stats FROM game_stats WHERE game_id = ? " +
                        "ORDER BY wins DESC, points DESC LIMIT ?")) {
            stmt.setString(1, gameId);
            stmt.setInt(2, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    top.add(new GameStats(UUID.fromString(rs.getString("uuid")), gameId,
                            rs.getInt("points"), rs.getInt("wins"), rs.getInt("losses"), parseOther(rs.getString("other_stats"))));
                }
            }
        }
        return Immutable.copy(top);
    }

    @Override
    public void addStat(UUID uuid, String username, String gameId, String key, int delta) throws SQLException {
        if (!key.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Invalid stat key: " + key);
        UserDataManager.getOrCreate(uuid, username);
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "INSERT OR IGNORE INTO games (game_id) VALUES (?)")) {
            stmt.setString(1, gameId);
            stmt.executeUpdate();
        }
        // Read-modify-write in Java: the server may bundle an old SQLite without the JSON functions or UPSERT.
        ensureRow(uuid, gameId);
        Map<String, Integer> other = new LinkedHashMap<String, Integer>(readOther(uuid, gameId));
        Integer current = other.get(key);
        other.put(key, (current == null ? 0 : current) + delta);
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "UPDATE game_stats SET other_stats = ? WHERE uuid = ? AND game_id = ?")) {
            stmt.setString(1, toJson(other));
            stmt.setString(2, uuid.toString());
            stmt.setString(3, gameId);
            stmt.executeUpdate();
        }
    }

    /** Creates the zeroed stats row if missing. INSERT OR IGNORE exists on every SQLite; never use REPLACE (it cascades). */
    private void ensureRow(UUID uuid, String gameId) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "INSERT OR IGNORE INTO game_stats (uuid, game_id, points, wins, losses) VALUES (?, ?, 0, 0, 0)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, gameId);
            stmt.executeUpdate();
        }
    }

    private Map<String, Integer> readOther(UUID uuid, String gameId) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "SELECT other_stats FROM game_stats WHERE uuid = ? AND game_id = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, gameId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? parseOther(rs.getString("other_stats")) : Collections.<String, Integer>emptyMap();
            }
        }
    }

    private static String toJson(Map<String, Integer> stats) {
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        for (Map.Entry<String, Integer> entry : stats.entrySet()) obj.addProperty(entry.getKey(), entry.getValue());
        return obj.toString();
    }

    @Override
    public int getStat(UUID uuid, String gameId, String key) throws SQLException {
        if (!key.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Invalid stat key: " + key);
        Integer value = readOther(uuid, gameId).get(key);
        return value == null ? 0 : value;
    }

    @Override
    public List<GameStats> getTopOverall(int limit) throws SQLException {
        List<GameStats> top = new ArrayList<>();
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "SELECT uuid, SUM(points) AS p, SUM(wins) AS w, SUM(losses) AS l FROM game_stats " +
                        "GROUP BY uuid ORDER BY w DESC, p DESC LIMIT ?")) {
            stmt.setInt(1, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    top.add(new GameStats(UUID.fromString(rs.getString("uuid")), "*",
                            rs.getInt("p"), rs.getInt("w"), rs.getInt("l")));
                }
            }
        }
        return Immutable.copy(top);
    }

    @Override
    public void addPoints(UUID uuid, String username, String gameId, int delta) throws SQLException {
        UserDataManager.getOrCreate(uuid, username);
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "INSERT OR IGNORE INTO games (game_id) VALUES (?)")) {
            stmt.setString(1, gameId);
            stmt.executeUpdate();
        }
        ensureRow(uuid, gameId);
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "UPDATE game_stats SET points = points + ? WHERE uuid = ? AND game_id = ?")) {
            stmt.setInt(1, delta);
            stmt.setString(2, uuid.toString());
            stmt.setString(3, gameId);
            stmt.executeUpdate();
        }
    }
}
