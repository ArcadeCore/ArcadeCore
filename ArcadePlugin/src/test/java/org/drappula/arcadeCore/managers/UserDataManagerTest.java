package org.drappula.arcadeCore.managers;

import org.drappula.arcadeApi.database.UserData;
import org.drappula.arcadeCore.ServerTest;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class UserDataManagerTest extends ServerTest {

    @Test
    void missingUserResolvesToEmpty() throws Exception {
        assertTrue(UserDataManager.get(UUID.randomUUID()).isEmpty());
    }

    @Test
    void getOrCreatePersistsAndRefreshesUsername() throws Exception {
        UUID uuid = UUID.randomUUID();

        UserData created = UserDataManager.getOrCreate(uuid, "old");
        assertEquals("old", created.getUsername());

        UserData refreshed = UserDataManager.getOrCreate(uuid, "new");
        assertEquals("new", refreshed.getUsername());

        Optional<UserData> stored = UserDataManager.get(uuid);
        assertTrue(stored.isPresent());
        assertEquals("new", stored.get().getUsername());
    }

    @Test
    void saveUpserts() throws Exception {
        UUID uuid = UUID.randomUUID();

        UserDataManager.save(new UserData(uuid, "first"));
        UserDataManager.save(new UserData(uuid, "second"));

        assertEquals("second", UserDataManager.get(uuid).orElseThrow().getUsername());
    }

    @Test
    void databaseFailureSurfacesAsRuntimeException() throws Exception {
        // Swap in a closed connection: statements fail with SQLException,
        // which getOrCreate must translate (and log via the plugin logger).
        java.lang.reflect.Field field =
                org.drappula.arcadeCore.database.Database.class.getDeclaredField("connection");
        field.setAccessible(true);
        java.sql.Connection live = (java.sql.Connection) field.get(null);
        live.close();
        try {
            UUID uuid = UUID.randomUUID();

            assertThrows(RuntimeException.class, () -> UserDataManager.getOrCreate(uuid, "name"));
            verify(core().getSLF4JLogger(), atLeastOnce()).error(
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any());
        } finally {
            org.drappula.arcadeCore.database.TestDb.disconnect();
            org.drappula.arcadeCore.database.TestDb.connect();
        }
    }
}
