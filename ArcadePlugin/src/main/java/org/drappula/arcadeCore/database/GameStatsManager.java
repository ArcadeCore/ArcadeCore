package org.drappula.arcadeCore.database;

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
                    "INSERT INTO games (game_id) VALUES (?) ON CONFLICT(game_id) DO NOTHING")) {
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
                try (PreparedStatement stmt = Database.get().prepareStatement(
                        "INSERT INTO game_stats (uuid, game_id, points, wins, losses) VALUES (?, ?, 0, ?, ?) " +
                                "ON CONFLICT(uuid, game_id) DO UPDATE SET " +
                                "wins = game_stats.wins + excluded.wins, " +
                                "losses = game_stats.losses + excluded.losses")) {
                    stmt.setString(1, entry.getKey().toString());
                    stmt.setString(2, gameId);
                    stmt.setInt(3, won ? 1 : 0);
                    stmt.setInt(4, won ? 0 : 1);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            ArcadeCore.get().getSLF4JLogger().error("Failed to record match result for game {}", match.getGame().getId(), e);
        }
    }

    @Override
    public Optional<GameStats> getStats(UUID uuid, String gameId) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "SELECT points, wins, losses FROM game_stats WHERE uuid = ? AND game_id = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, gameId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new GameStats(uuid, gameId,
                        rs.getInt("points"), rs.getInt("wins"), rs.getInt("losses")));
            }
        }
    }

    @Override
    public List<GameStats> getTopPlayers(String gameId, int limit) throws SQLException {
        List<GameStats> top = new ArrayList<>();
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "SELECT uuid, points, wins, losses FROM game_stats WHERE game_id = ? " +
                        "ORDER BY wins DESC, points DESC LIMIT ?")) {
            stmt.setString(1, gameId);
            stmt.setInt(2, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    top.add(new GameStats(UUID.fromString(rs.getString("uuid")), gameId,
                            rs.getInt("points"), rs.getInt("wins"), rs.getInt("losses")));
                }
            }
        }
        return List.copyOf(top);
    }

    @Override
    public void addPoints(UUID uuid, String username, String gameId, int delta) throws SQLException {
        UserDataManager.getOrCreate(uuid, username);
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "INSERT INTO games (game_id) VALUES (?) ON CONFLICT(game_id) DO NOTHING")) {
            stmt.setString(1, gameId);
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "INSERT INTO game_stats (uuid, game_id, points, wins, losses) VALUES (?, ?, ?, 0, 0) " +
                        "ON CONFLICT(uuid, game_id) DO UPDATE SET points = game_stats.points + excluded.points")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, gameId);
            stmt.setInt(3, delta);
            stmt.executeUpdate();
        }
    }
}
