package org.drappula.arcadeCore;

import org.drappula.arcadeCore.config.DataConfig;
import org.drappula.arcadeCore.config.MainConfig;
import org.drappula.arcadeCore.config.MessagesConfig;
import org.drappula.arcadeCore.database.TestDb;
import org.drappula.arcadeCore.managers.ProfileManager;
import org.drappula.arcadeCore.managers.game.GameManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Comparator;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Full-server test harness. Boots MockBukkit, injects an in-memory database,
 * and stands up a mocked {@link ArcadeCore} backed by real config files so
 * manager/scheduler/config paths run for real. Test-only code; production
 * classes are untouched (all wiring here uses reflection and mocks).
 */
public abstract class ServerTest {

    protected ServerMock server;
    private ArcadeCore coreMock;
    private Path dataFolder;

    @BeforeEach
    void bootServer() throws Exception {
        server = MockBukkit.mock();
        TestDb.connect();
        GameManager.get().reload();

        dataFolder = Files.createTempDirectory("arcade-test");
        coreMock = mock(ArcadeCore.class);
        when(coreMock.getDataFolder()).thenReturn(dataFolder.toFile());
        when(coreMock.getSLF4JLogger()).thenReturn(mock(net.kyori.adventure.text.logger.slf4j.ComponentLogger.class));
        when(coreMock.getResource(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            InputStream stream = ServerTest.class.getResourceAsStream("/" + name);
            if (stream == null) {
                stream = ServerTest.class.getResourceAsStream("/paper-plugin.yml");
            }
            return stream;
        });
        setCore(coreMock);

        DataConfig.setup();
        MainConfig.setup();
        MessagesConfig.setup();
    }

    @AfterEach
    void tearDownServer() throws Exception {
        GameManager.get().reload();
        clearManagerState();
        setCore(null);
        TestDb.disconnect();
        MockBukkit.unmock();
        if (dataFolder != null) {
            try (var walk = Files.walk(dataFolder)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }

    protected ArcadeCore core() {
        return coreMock;
    }

    protected Connection connection() throws Exception {
        Field field = org.drappula.arcadeCore.database.Database.class.getDeclaredField("connection");
        field.setAccessible(true);
        return (Connection) field.get(null);
    }

    private static void setCore(ArcadeCore core) throws Exception {
        Field field = ArcadeCore.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, core);
    }

    /** Clears static manager state so tests never leak into each other. */
    @SuppressWarnings("unchecked")
    private static void clearManagerState() throws Exception {
        for (org.drappula.arcadeApi.systems.IProfile profile : ProfileManager.get().all()) {
            ProfileManager.removeProfile(profile.getPlayer());
        }
        clearField(org.drappula.arcadeCore.managers.queue.QueueManager.get(), "queues");
        clearField(org.drappula.arcadeCore.managers.queue.QueueManager.get(), "countdowns");
        clearSet(org.drappula.arcadeCore.managers.world.ArenaWorldManager.get(), "arenaWorlds");
        ((java.util.List<?>) mapList()).clear();
    }

    private static Object mapList() throws Exception {
        Field field = org.drappula.arcadeCore.managers.map.MapManager.class.getDeclaredField("maps");
        field.setAccessible(true);
        return field.get(org.drappula.arcadeCore.managers.map.MapManager.get());
    }

    @SuppressWarnings("unchecked")
    private static void clearField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        ((java.util.Map<?, ?>) field.get(target)).clear();
    }

    private static void clearSet(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        ((java.util.Set<?>) field.get(target)).clear();
    }
}
