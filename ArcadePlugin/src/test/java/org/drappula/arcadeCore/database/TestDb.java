package org.drappula.arcadeCore.database;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Test-only SQLite setup. Injects an isolated in-memory database into the
 * static {@link Database} connection via reflection, so database-backed
 * managers can be tested without a running server. Production code is
 * untouched: all reflection lives here.
 *
 * <p>Schema mirrors {@code Database.connect()} exactly; if the production
 * schema drifts, these tests fail loudly.
 */
public final class TestDb {
    private TestDb() {
    }

    public static Connection connect() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement pragma = connection.createStatement()) {
            pragma.execute("PRAGMA foreign_keys = ON;");
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
        Field field = Database.class.getDeclaredField("connection");
        field.setAccessible(true);
        field.set(null, connection);
        return connection;
    }

    public static void disconnect() throws Exception {
        Field field = Database.class.getDeclaredField("connection");
        field.setAccessible(true);
        Connection connection = (Connection) field.get(null);
        field.set(null, null);
        if (connection != null && !connection.isClosed()) connection.close();
    }
}
