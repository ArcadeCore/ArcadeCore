package org.drappula.arcadeCore.managers;

import org.drappula.arcadeCore.util.Log;
import org.drappula.arcadeApi.database.UserData;
import org.drappula.arcadeCore.ArcadeCore;
import org.drappula.arcadeCore.database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class UserDataManager {
    public static Optional<UserData> get(UUID uuid) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "SELECT username FROM user_profiles WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new UserData(uuid, rs.getString("username")));
            }
        }
    }

    public static UserData getOrCreate(UUID uuid, String username) {
        try {
            Optional<UserData> existing = get(uuid);
            if (existing.isPresent()) {
                UserData profile = existing.get();
                profile.setUsername(username);
                save(profile);
                return profile;
            }
            UserData profile = new UserData(uuid, username);
            save(profile);
            return profile;
        } catch (SQLException e) {
            Log.error("An error occurred while trying to get or create user {} ({})", username, uuid);
            throw new RuntimeException(e);
        }
    }


    public static void save(UserData profile) throws SQLException {
        // INSERT OR IGNORE + UPDATE instead of UPSERT (needs SQLite 3.24) or REPLACE (cascades into game_stats).
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "INSERT OR IGNORE INTO user_profiles (uuid, username) VALUES (?, ?)")) {
            stmt.setString(1, profile.getUuid().toString());
            stmt.setString(2, profile.getUsername());
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "UPDATE user_profiles SET username = ? WHERE uuid = ?")) {
            stmt.setString(1, profile.getUsername());
            stmt.setString(2, profile.getUuid().toString());
            stmt.executeUpdate();
        }
    }
}
