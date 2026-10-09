package org.drappula.arcadeCore.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import org.drappula.arcadeApi.message.LegacyText;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.map.MapConfigOption;
import org.drappula.arcadeCore.ServerTest;
import org.drappula.arcadeCore.managers.game.GameManager;
import org.drappula.arcadeCore.managers.map.MapManager;
import org.drappula.arcadeCore.managers.queue.QueueManager;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Exercises every {@code /arcade} handler through the Bukkit executor and tab completer. */
class MainCommandTest extends ServerTest {

    private static class FakeGame implements Game {
        @Override public String getId() { return "game"; }
        @Override public String getDisplayName() { return "Game"; }
        @Override public int getPlayersRequired() { return 1; }
        @Override public int getMinPlayers() { return 5; }
        @Override public int getMaxPlayers() { return 8; }
    }

    private static class FakeOptionGame extends FakeGame {
        @Override
        public List<MapConfigOption> getMapConfigOptions() {
            return Arrays.asList(MapConfigOption.integer("speed", 5, 0, 60), MapConfigOption.text("motd", "hello"));
        }
    }

    private static final MainCommand COMMAND = new MainCommand();

    private static boolean run(CommandSender sender, String... args) {
        return COMMAND.onCommand(sender, mock(Command.class), "arcade", args);
    }

    private static List<String> tab(CommandSender sender, String... args) {
        return COMMAND.onTabComplete(sender, mock(Command.class), "arcade", args);
    }

    private static CommandSender admin() {
        CommandSender admin = mock(CommandSender.class);
        when(admin.hasPermission(anyString())).thenReturn(true);
        return admin;
    }

    private static CommandSender nobody() {
        CommandSender nobody = mock(CommandSender.class);
        when(nobody.hasPermission(anyString())).thenReturn(false);
        return nobody;
    }

    private PlayerMock adminPlayer() {
        PlayerMock player = server.addPlayer();
        player.setOp(true);
        return player;
    }

    /** Everything a mocked sender received as chat, colour codes stripped. */
    private static String received(CommandSender sender) {
        org.mockito.ArgumentCaptor<String> sent = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).sendMessage(sent.capture());
        return LegacyText.strip(String.join("\n", sent.getAllValues()));
    }

    private static String next(PlayerMock player) {
        String message = player.nextMessage();
        assertNotNull(message, "player received no message");
        return LegacyText.strip(message);
    }

    @Test
    void infoShowsPluginMeta() {
        when(core().getDescription()).thenReturn(new PluginDescriptionFile("ArcadeCore", "1.0.0", "x.Main"));
        CommandSender sender = mock(CommandSender.class);

        run(sender);

        String text = received(sender);
        assertTrue(text.contains("ArcadeCore") && text.contains("1.0.0"));
    }

    @Test
    void reloadSucceeds() {
        CommandSender sender = mock(CommandSender.class);

        run(sender, "reload");

        assertTrue(received(sender).contains("Reloaded the plugin configuration."));
    }

    @Test
    void setSpawnPersistsLocation() {
        PlayerMock player = server.addPlayer();
        org.bukkit.Location at = player.getLocation().add(5, 0, 0);
        player.teleport(at);

        run(player, "setspawn");

        java.util.Optional<org.bukkit.Location> loaded = org.drappula.arcadeCore.config.DataConfig.getSpawnLocation();
        assertTrue(loaded.isPresent());
        assertEquals(5, loaded.get().getX());
        assertNotNull(player.nextMessage());
    }

    @Test
    void setSpawnRejectsConsole() {
        CommandSender console = mock(CommandSender.class);

        run(console, "setspawn");

        assertTrue(received(console).contains("Only players"));
    }

    @Test
    void startRequiresPermission() {
        GameManager.get().registerGame(new FakeGame());
        CommandSender sender = nobody();

        run(sender, "start", "game");

        assertTrue(received(sender).contains("permission"));
    }

    @Test
    void startUnknownGame() {
        CommandSender sender = admin();

        run(sender, "start", "missing");

        assertTrue(received(sender).contains("Unknown game: missing"));
    }

    @Test
    void startEmptyQueue() {
        GameManager.get().registerGame(new FakeGame());
        CommandSender sender = admin();

        run(sender, "start", "game");

        assertTrue(received(sender).contains("No players are queued for that game."));
    }

    @Test
    void startForceStartsQueuedMatch() {
        GameManager.get().registerGame(new FakeGame());
        PlayerMock player = server.addPlayer();
        QueueManager.get().joinQueue(player, GameManager.get().getGame("game"));

        run(admin(), "start", "game");

        // No maps: the queued players are requeued, nothing is lost.
        assertTrue(QueueManager.get().isQueued(player));
    }

    @Test
    void queueUnknownGame() {
        PlayerMock player = server.addPlayer();

        run(player, "queue", "missing");

        assertFalse(QueueManager.get().isQueued(player));
    }

    @Test
    void queueRejectsConsole() {
        GameManager.get().registerGame(new FakeGame());
        CommandSender console = mock(CommandSender.class);

        run(console, "queue", "game");

        assertTrue(received(console).contains("Only players can join the queue."));
    }

    @Test
    void queueJoinsPlayer() {
        GameManager.get().registerGame(new FakeGame());
        PlayerMock player = server.addPlayer();

        run(player, "queue", "game");

        assertTrue(QueueManager.get().isQueued(player));
        assertNotNull(player.nextMessage());
    }

    @Test
    void queueDeniedWhenAlreadyQueued() {
        GameManager.get().registerGame(new FakeGame());
        PlayerMock player = server.addPlayer();
        QueueManager.get().joinQueue(player, GameManager.get().getGame("game"));
        player.nextMessage();

        run(player, "queue", "game");

        assertNotNull(player.nextMessage());
    }

    @Test
    void mapCommandsRequirePermission() {
        CommandSender sender = nobody();

        run(sender, "map", "list");

        assertTrue(received(sender).contains("permission"));
    }

    @Test
    void mapCreateUnknownGame() {
        PlayerMock player = adminPlayer();

        run(player, "map", "create", "arena", "missing");

        assertNull(MapManager.get().getMap("arena"));
    }

    @Test
    void mapCreateStripsMessagePrefix() {
        GameManager.get().registerGame(new FakeGame());
        PlayerMock player = adminPlayer();

        run(player, "map", "create", "arena", "game");

        String text = next(player);
        assertTrue(text.contains("Created map arena"));
        assertFalse(text.contains("message:"));
    }

    @Test
    void unknownGameStripsPrefixForConsole() {
        CommandSender console = admin();

        run(console, "queue", "missing");

        String text = received(console);
        assertTrue(text.contains("Unknown game"));
        assertFalse(text.contains("message:"));
    }

    @Test
    void mapCreateAndList() {
        GameManager.get().registerGame(new FakeGame());
        run(adminPlayer(), "map", "create", "arena", "game");
        assertNotNull(MapManager.get().getMap("arena"));
        CommandSender sender = admin();

        run(sender, "map", "list", "game");

        assertTrue(received(sender).contains("arena"));
    }

    @Test
    void mapListWithoutFilterShowsAll() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        CommandSender sender = admin();

        run(sender, "map", "list");

        assertTrue(received(sender).contains("arena"));
    }

    @Test
    void mapListMarksDisabledMaps() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        MapManager.get().setEnabled("arena", false);
        CommandSender sender = admin();

        run(sender, "map", "list", "game");

        assertTrue(received(sender).contains("[disabled]"));
    }

    @Test
    void reloadFailureReportsAndRethrows() {
        CommandSender sender = mock(CommandSender.class);
        java.io.File dataFile = new java.io.File(core().getDataFolder(), "data.yml");
        assertTrue(dataFile.delete());
        assertTrue(dataFile.mkdir());

        try {
            assertThrows(RuntimeException.class, () -> run(sender, "reload"));
            assertTrue(received(sender).contains("Failed to reload the plugin!"));
        } finally {
            assertTrue(dataFile.delete());
        }
    }

    @Test
    void mapAddSpawnUnknownMap() {
        PlayerMock player = adminPlayer();

        run(player, "map", "addspawn", "missing");

        assertNotNull(player.nextMessage());
    }

    @Test
    void mapAddSpawnRejectsNonPlayerSender() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");

        run(admin(), "map", "addspawn", "arena");

        assertEquals(0, MapManager.get().getMap("arena").getSpawnPoints().size());
    }

    @Test
    void mapAddSpawnStoresClone() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");

        run(adminPlayer(), "map", "addspawn", "arena");

        assertEquals(1, MapManager.get().getMap("arena").getSpawnPoints().size());
    }

    @Test
    void mapSetEnabledToggles() throws Exception {
        MapManager.get().createMap("arena", "game", "Arena", "world");

        run(admin(), "map", "disable", "arena");
        assertFalse(MapManager.get().getMap("arena").isEnabled());

        run(admin(), "map", "enable", "arena");
        assertTrue(MapManager.get().getMap("arena").isEnabled());
    }

    @Test
    void mapSetEnabledUnknownMap() {
        CommandSender sender = admin();

        run(sender, "map", "enable", "missing");

        String text = received(sender);
        assertTrue(text.contains("Unknown map"));
        assertFalse(text.contains("message:"));
    }

    @Test
    void mapDeleteRemoves() throws Exception {
        MapManager.get().createMap("arena", "game", "Arena", "world");

        run(admin(), "map", "delete", "arena");

        assertNull(MapManager.get().getMap("arena"));
    }

    @Test
    void mapDeleteUnknownMap() {
        CommandSender sender = admin();

        run(sender, "map", "delete", "missing");

        String text = received(sender);
        assertTrue(text.contains("Unknown map"));
        assertFalse(text.contains("message:"));
    }

    @Test
    void databaseFailurePathsReportToSender() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        PlayerMock player = adminPlayer();
        // Break the database: every map write below hits the SQLException catch.
        java.lang.reflect.Field field =
                org.drappula.arcadeCore.database.Database.class.getDeclaredField("connection");
        field.setAccessible(true);
        ((java.sql.Connection) field.get(null)).close();

        run(player, "map", "addspawn", "arena");
        CommandSender sender = admin();
        run(sender, "map", "enable", "arena");
        run(sender, "map", "delete", "arena");
        run(player, "map", "create", "other", "game");

        assertNotNull(player.nextMessage());
        assertTrue(received(sender).contains("Failed"));
    }

    @Test
    void mapConfigSetPersistsCanonicalValue() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        PlayerMock player = adminPlayer();

        run(player, "map", "config", "arena", "set", "speed", "007");

        assertEquals("7", MapManager.get().getMap("arena").getConfigOverrides().get("speed"));
        String text = next(player);
        assertTrue(text.contains("speed") && text.contains("7"));
        assertFalse(text.contains("message:"));
    }

    @Test
    void mapConfigSetKeepsSpacesInTextValues() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");

        run(admin(), "map", "config", "arena", "set", "motd", "hello", "big", "world");

        assertEquals("hello big world", MapManager.get().getMap("arena").getConfigOverrides().get("motd"));
    }

    @Test
    void mapConfigSetUnknownMap() {
        CommandSender sender = admin();

        run(sender, "map", "config", "missing", "set", "speed", "5");

        assertTrue(received(sender).contains("Unknown map"));
    }

    @Test
    void mapConfigSetUnknownKeyListsRegistered() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        CommandSender sender = admin();

        run(sender, "map", "config", "arena", "set", "nope", "5");

        String text = received(sender);
        assertTrue(text.contains("nope") && text.contains("speed"));
    }

    @Test
    void mapConfigSetInvalidValueRejected() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        CommandSender sender = admin();

        run(sender, "map", "config", "arena", "set", "speed", "999");
        run(sender, "map", "config", "arena", "set", "speed", "fast");

        assertNotEquals("999", MapManager.get().getMap("arena").getConfigOverrides().get("speed"));
        assertTrue(received(sender).contains("Invalid value"));
    }

    @Test
    void mapConfigSetRejectsWhenGameHasNoOptions() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");

        run(admin(), "map", "config", "arena", "set", "speed", "5");

        assertTrue(MapManager.get().getMap("arena").getConfigOverrides().isEmpty());
    }

    @Test
    void mapConfigUnsetRemovesOverride() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        MapManager.get().setMapConfig("arena", "speed", "42");
        PlayerMock player = adminPlayer();

        run(player, "map", "config", "arena", "unset", "speed");

        assertFalse(MapManager.get().getMap("arena").getConfigOverrides().containsKey("speed"));
        assertTrue(next(player).contains("speed"));
    }

    @Test
    void mapConfigUnsetWithoutOverrideIsInfo() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        org.drappula.arcadeCore.database.MapDataManager.deleteConfig("arena", "speed");
        MapManager.get().getMap("arena").getConfigInternal().remove("speed");
        CommandSender sender = admin();

        run(sender, "map", "config", "arena", "unset", "speed");

        assertTrue(received(sender).contains("no override"));
    }

    @Test
    void mapConfigListShowsEffectiveValues() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        MapManager.get().setMapConfig("arena", "speed", "42");
        CommandSender sender = admin();

        run(sender, "map", "config", "arena", "list");

        String text = received(sender);
        assertTrue(text.contains("speed=42") && text.contains("motd=hello"));
    }

    @Test
    void unknownSubcommandShowsUsage() {
        CommandSender sender = admin();

        run(sender, "bogus");

        assertTrue(received(sender).contains("Usage"));
    }

    @Test
    void tabCompletesSubcommandsAndFiltersByPrefix() {
        assertTrue(tab(admin(), "").containsAll(Arrays.asList("reload", "setspawn", "start", "queue", "map")));
        assertEquals(Arrays.asList("reload"), tab(admin(), "re"));
    }

    @Test
    void tabCompletesGamesMapsAndConfigKeys() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");

        assertTrue(tab(admin(), "start", "").contains("game"));
        assertTrue(tab(admin(), "map", "enable", "").contains("arena"));
        assertTrue(tab(admin(), "map", "create", "x", "").contains("game"));
        assertEquals(Arrays.asList("set", "unset", "list"), tab(admin(), "map", "config", "arena", ""));
        List<String> keys = tab(admin(), "map", "config", "arena", "set", "");
        assertTrue(keys.contains("speed") && keys.contains("motd"));
    }

    @Test
    void tabHidesMapSubcommandsWithoutPermission() {
        assertTrue(tab(nobody(), "map", "").isEmpty());
    }
}
