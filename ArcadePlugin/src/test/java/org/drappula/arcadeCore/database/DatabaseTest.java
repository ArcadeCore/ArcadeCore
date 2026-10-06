package org.drappula.arcadeCore.database;

import org.drappula.arcadeCore.ServerTest;
import org.drappula.arcadeCore.managers.UserDataManager;
import org.junit.jupiter.api.Test;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest extends ServerTest {

    @Test
    void connectCreatesFileAndSchema() throws Exception {
        // ServerTest injects an in-memory DB; reconnect for real to test file setup.
        TestDb.disconnect();
        try {
            Database.connect();

            assertNotNull(Database.get());
            assertFalse(Database.get().isClosed());

            Set<String> tables = new HashSet<>();
            DatabaseMetaData meta = Database.get().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) tables.add(rs.getString("TABLE_NAME"));
            }
            assertTrue(tables.containsAll(
                    Set.of("user_profiles", "games", "game_stats", "maps", "map_spawns")));

            // Round-trip through the real file database.
            UUID uuid = UUID.randomUUID();
            UserDataManager.save(new org.drappula.arcadeApi.database.UserData(uuid, "file"));
            assertEquals("file", UserDataManager.get(uuid).orElseThrow().getUsername());

            assertTrue(core().getDataFolder().toPath().resolve("database.db").toFile().exists());
        } finally {
            Database.disconnect();
            TestDb.connect();
        }
    }

    @Test
    void disconnectClosesConnection() throws Exception {
        TestDb.disconnect();
        try {
            Database.connect();
            Database.disconnect();

            assertTrue(Database.get().isClosed());
        } finally {
            TestDb.connect();
        }
    }

    @Test
    void disconnectWithoutConnectionIsSafe() throws Exception {
        TestDb.disconnect();

        assertDoesNotThrow(Database::disconnect);
    }
}
