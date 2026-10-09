package org.drappula.arcadeCore.database;

import org.drappula.arcadeCore.ArcadeCore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static Connection connection;

    public static void connect() throws SQLException {
        ArcadeCore.get().getDataFolder().mkdirs();
        try {
            // Explicit load: DriverManager's service lookup does not see drivers inside a plugin classloader on old servers.
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver is missing from the plugin jar", e);
        }
        connection = DriverManager.getConnection(
                "jdbc:sqlite:" + ArcadeCore.get().getDataFolder().getAbsolutePath() + "/database.db");
        try (Statement pragmaStmt = connection.createStatement()) {
            pragmaStmt.execute("PRAGMA foreign_keys = ON;");
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS user_profiles (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "username TEXT NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS games (" +
                    "game_id TEXT PRIMARY KEY)");
            stmt.execute("CREATE TABLE IF NOT EXISTS game_stats (" +
                    "uuid TEXT, " +
                    "game_id TEXT, " +
                    "points INT, " +
                    "wins INT, " +
                    "losses INT, " +
                    "other_stats JSON, " +
                    "PRIMARY KEY (uuid, game_id), " +
                    "FOREIGN KEY (uuid) REFERENCES user_profiles(uuid) ON DELETE CASCADE, " +
                    "FOREIGN KEY (game_id) REFERENCES games(game_id) ON DELETE CASCADE)");
            stmt.execute("CREATE TABLE IF NOT EXISTS maps (" +
                    "map_id TEXT PRIMARY KEY, " +
                    "game_id TEXT NOT NULL, " +
                    "display_name TEXT NOT NULL, " +
                    "world TEXT NOT NULL, " +
                    "enabled INTEGER NOT NULL DEFAULT 1, " +
                    "in_use INTEGER NOT NULL DEFAULT 0)");
            stmt.execute("CREATE TABLE IF NOT EXISTS map_spawns (" +
                    "map_id TEXT NOT NULL, " +
                    "spawn_index INTEGER NOT NULL, " +
                    "x REAL NOT NULL, " +
                    "y REAL NOT NULL, " +
                    "z REAL NOT NULL, " +
                    "yaw REAL NOT NULL, " +
                    "pitch REAL NOT NULL, " +
                    "PRIMARY KEY (map_id, spawn_index), " +
                    "FOREIGN KEY (map_id) REFERENCES maps(map_id) ON DELETE CASCADE)");
            stmt.execute("CREATE TABLE IF NOT EXISTS map_config (" +
                    "map_id TEXT NOT NULL, " +
                    "key TEXT NOT NULL, " +
                    "value TEXT NOT NULL, " +
                    "PRIMARY KEY (map_id, key), " +
                    "FOREIGN KEY (map_id) REFERENCES maps(map_id) ON DELETE CASCADE)");
        }
    }

    public static Connection get() {
        return connection;
    }

    public static void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
