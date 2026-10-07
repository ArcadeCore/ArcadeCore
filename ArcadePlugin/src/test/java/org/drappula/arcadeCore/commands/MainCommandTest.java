package org.drappula.arcadeCore.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeCore.ServerTest;
import org.drappula.arcadeCore.managers.game.GameManager;
import org.drappula.arcadeCore.managers.map.MapManager;
import org.drappula.arcadeCore.managers.queue.QueueManager;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Exercises every {@code /arcade} handler by invoking the private static
 * methods with mocked Brigadier contexts (test-only reflection; the command
 * tree wiring itself is covered by {@link #buildsCommandTree}).
 */
class MainCommandTest extends ServerTest {

    private static class FakeGame implements Game {        @Override
        public String getId() {
            return "game";
        }

        @Override
        public String getDisplayName() {
            return "Game";
        }

        @Override
        public int getPlayersRequired() {
            return 1;
        }

        @Override
        public int getMinPlayers() {
            return 5;
        }

        @Override
        public int getMaxPlayers() {
            return 8;
        }
    }

    private static class FakeOptionGame extends FakeGame {
        @Override
        public java.util.List<org.drappula.arcadeApi.systems.map.MapConfigOption> getMapConfigOptions() {
            return java.util.List.of(
                    org.drappula.arcadeApi.systems.map.MapConfigOption.integer("speed", 5, 0, 60),
                    org.drappula.arcadeApi.systems.map.MapConfigOption.text("motd", "hello"));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String plainText(Component message) {
        return PlainTextComponentSerializer.plainText().serialize(message);
    }

    private static CommandContext<CommandSourceStack> ctx(String game, String mapId,
                                                          CommandSourceStack stack) {
        CommandContext ctx = mock(CommandContext.class);
        when(ctx.getSource()).thenReturn(stack);
        if (game != null) when(ctx.getArgument(eq("game"), eq(String.class))).thenReturn(game);
        if (mapId != null) when(ctx.getArgument(eq("mapId"), eq(String.class))).thenReturn(mapId);
        return ctx;
    }

    private static CommandSourceStack stack(CommandSender sender, org.bukkit.Location location) {
        CommandSourceStack stack = mock(CommandSourceStack.class);
        when(stack.getSender()).thenReturn(sender);
        when(stack.getLocation()).thenReturn(location);
        return stack;
    }

    private static CommandContext<CommandSourceStack> configCtx(String mapId, String key, String value,
                                                               CommandSender sender) {
        CommandContext ctx = mock(CommandContext.class);
        CommandSourceStack stack = stack(sender, null);
        when(ctx.getSource()).thenReturn(stack);
        if (mapId != null) when(ctx.getArgument(eq("mapId"), eq(String.class))).thenReturn(mapId);
        if (key != null) when(ctx.getArgument(eq("key"), eq(String.class))).thenReturn(key);
        if (value != null) when(ctx.getArgument(eq("value"), eq(String.class))).thenReturn(value);
        return ctx;
    }

    private static int invoke(String method, CommandContext<CommandSourceStack> ctx) throws Exception {
        Method handler = MainCommand.class.getDeclaredMethod(method, CommandContext.class);
        handler.setAccessible(true);
        return (int) handler.invoke(null, ctx);
    }

    private static int invoke(String method, CommandContext<CommandSourceStack> ctx, boolean enabled)
            throws Exception {
        Method handler = MainCommand.class.getDeclaredMethod(method, CommandContext.class, boolean.class);
        handler.setAccessible(true);
        return (int) handler.invoke(null, ctx, enabled);
    }

    @Test
    void buildsCommandTree() {
        assertNotNull(new MainCommand());
        assertEquals("arcade", MainCommand.get().getLiteral());
    }

    @Test
    void dispatchesThroughBuiltTree() {
        GameManager.get().registerGame(new FakeGame());
        PlayerMock player = server.addPlayer();
        var meta = mock(io.papermc.paper.plugin.configuration.PluginMeta.class);
        when(meta.getDisplayName()).thenReturn("ArcadeCore");
        when(meta.getDescription()).thenReturn("desc");
        when(meta.getVersion()).thenReturn("1.0.0");
        when(core().getPluginMeta()).thenReturn(meta);
        var dispatcher = new com.mojang.brigadier.CommandDispatcher<CommandSourceStack>();
        dispatcher.getRoot().addChild(MainCommand.get());
        CommandSourceStack stack = stack(player, player.getLocation());

        assertDoesNotThrow(() -> dispatcher.execute("arcade", stack));
        assertDoesNotThrow(() -> dispatcher.execute("arcade reload", stack));
    }

    @Test
    void infoShowsPluginMeta() {
        var meta = mock(io.papermc.paper.plugin.configuration.PluginMeta.class);
        when(meta.getDisplayName()).thenReturn("ArcadeCore");
        when(meta.getDescription()).thenReturn("desc");
        when(meta.getVersion()).thenReturn("1.0.0");
        when(core().getPluginMeta()).thenReturn(meta);
        CommandSender sender = mock(CommandSender.class);

        assertDoesNotThrow(() -> invoke("info", ctx(null, null, stack(sender, null))));
        verify(sender, times(1)).sendRichMessage(anyString(), any());
    }

    @Test
    void reloadSucceeds() {
        CommandSender sender = mock(CommandSender.class);

        assertDoesNotThrow(() -> invoke("reload", ctx(null, null, stack(sender, null))));
        verify(sender).sendRichMessage(eq("<green>Reloaded the plugin configuration."));
    }

    @Test
    void setSpawnPersistsLocation() throws Exception {
        PlayerMock player = server.addPlayer();
        org.bukkit.Location at = player.getLocation().add(5, 0, 0);

        invoke("setSpawn", ctx(null, null, stack(player, at)));

        var loaded = org.drappula.arcadeCore.config.DataConfig.getSpawnLocation();
        assertTrue(loaded.isPresent());
        assertEquals(5, loaded.get().getX());
        assertNotNull(player.nextComponentMessage());
    }

    @Test
    void startUnknownGame() throws Exception {
        CommandSender sender = mock(CommandSender.class);

        invoke("start", ctx("missing", null, stack(sender, null)));

        verify(sender).sendRichMessage(eq("<red>Unknown game: <gray><id></gray>"), any());
    }

    @Test
    void startEmptyQueue() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        CommandSender sender = mock(CommandSender.class);

        invoke("start", ctx("game", null, stack(sender, null)));

        verify(sender).sendRichMessage(eq("<red>No players are queued for that game."));
    }

    @Test
    void startForceStartsQueuedMatch() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        PlayerMock player = server.addPlayer();
        QueueManager.get().joinQueue(player, GameManager.get().getGame("game"));
        CommandSender sender = mock(CommandSender.class);

        invoke("start", ctx("game", null, stack(sender, null)));

        // No maps: the queued players are requeued, nothing is lost.
        assertTrue(QueueManager.get().isQueued(player));
    }

    @Test
    void queueUnknownGame() throws Exception {
        PlayerMock player = server.addPlayer();

        invoke("queue", ctx("missing", null, stack(player, player.getLocation())));

        assertFalse(QueueManager.get().isQueued(player));
    }

    @Test
    void queueRejectsConsole() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        CommandSender console = mock(CommandSender.class);

        invoke("queue", ctx("game", null, stack(console, null)));

        verify(console).sendRichMessage(eq("<red>Only players can join the queue."));
    }

    @Test
    void queueJoinsPlayer() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        PlayerMock player = server.addPlayer();

        invoke("queue", ctx("game", null, stack(player, player.getLocation())));

        assertTrue(QueueManager.get().isQueued(player));
        assertNotNull(player.nextComponentMessage());
    }

    @Test
    void queueDeniedWhenAlreadyQueued() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        PlayerMock player = server.addPlayer();
        QueueManager.get().joinQueue(player, GameManager.get().getGame("game"));
        player.nextComponentMessage();

        invoke("queue", ctx("game", null, stack(player, player.getLocation())));

        assertNotNull(player.nextComponentMessage());
    }

    @Test
    void mapCreateUnknownGame() throws Exception {
        PlayerMock player = server.addPlayer();

        invoke("mapCreate", ctx("missing", "arena", stack(player, player.getLocation())));

        assertNull(MapManager.get().getMap("arena"));
    }

    @Test
    void mapCreateStripsMessagePrefix() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        PlayerMock player = server.addPlayer();

        invoke("mapCreate", ctx("game", "arena", stack(player, player.getLocation())));

        String text = plainText(player.nextComponentMessage());
        assertTrue(text.contains("Created map arena"));
        assertFalse(text.contains("message:"));
    }

    @Test
    void unknownGameStripsPrefixForConsole() throws Exception {
        invoke("queue", ctx("missing", null, stack(server.getConsoleSender(), null)));

        String text = plainText(server.getConsoleSender().nextComponentMessage());
        assertTrue(text.contains("Unknown game"));
        assertFalse(text.contains("message:"));
    }

    @Test
    void mapCreateAndList() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        PlayerMock player = server.addPlayer();

        invoke("mapCreate", ctx("game", "arena", stack(player, player.getLocation())));

        assertNotNull(MapManager.get().getMap("arena"));
        CommandSender sender = mock(CommandSender.class);
        invoke("mapList", ctx("game", null, stack(sender, null)));
        verify(sender).sendRichMessage(argThat(message -> message.contains("arena")));
    }

    @Test
    void mapListWithoutFilterShowsAll() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        CommandSender sender = mock(CommandSender.class);

        invoke("mapList", ctx(null, null, stack(sender, null)));

        verify(sender).sendRichMessage(argThat(message -> message.contains("arena")));
    }

    @Test
    void mapListMarksDisabledMaps() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        MapManager.get().setEnabled("arena", false);
        CommandSender sender = mock(CommandSender.class);

        invoke("mapList", ctx("game", null, stack(sender, null)));

        verify(sender).sendRichMessage(argThat(message -> message.contains("[disabled]")));
    }

    @Test
    void reloadFailureReportsAndRethrows() throws Exception {
        CommandSender sender = mock(CommandSender.class);
        java.io.File dataFile = new java.io.File(core().getDataFolder(), "data.yml");
        assertTrue(dataFile.delete());
        assertTrue(dataFile.mkdir());

        try {
            assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> invoke("reload", ctx(null, null, stack(sender, null))));
            verify(sender).sendRichMessage(eq("<red>Failed to reload the plugin! See the console for more details."));
        } finally {
            assertTrue(dataFile.delete());
        }
    }

    @Test
    void mapAddSpawnUnknownMap() throws Exception {
        PlayerMock player = server.addPlayer();

        invoke("mapAddSpawn", ctx(null, "missing", stack(player, player.getLocation())));

        assertNotNull(player.nextComponentMessage());
    }

    @Test
    void mapAddSpawnRejectsNonPlayerSender() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        PlayerMock player = server.addPlayer();

        invoke("mapAddSpawn", ctx(null, "arena", stack(server.getConsoleSender(), player.getLocation())));

        assertEquals(0, MapManager.get().getMap("arena").getSpawnPoints().size());
    }

    @Test
    void mapAddSpawnStoresClone() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        PlayerMock player = server.addPlayer();

        invoke("mapAddSpawn", ctx(null, "arena", stack(player, player.getLocation())));

        assertEquals(1, MapManager.get().getMap("arena").getSpawnPoints().size());
    }

    @Test
    void mapSetEnabledToggles() throws Exception {
        MapManager.get().createMap("arena", "game", "Arena", "world");
        CommandSender sender = mock(CommandSender.class);

        invoke("mapSetEnabled", ctx(null, "arena", stack(sender, null)), false);

        assertFalse(MapManager.get().getMap("arena").isEnabled());
    }

    @Test
    void mapSetEnabledUnknownMap() throws Exception {
        CommandSender sender = mock(CommandSender.class);

        invoke("mapSetEnabled", ctx(null, "missing", stack(sender, null)), true);

        org.mockito.ArgumentCaptor<Component> sent = org.mockito.ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(sent.capture());
        String text = plainText(sent.getValue());
        assertTrue(text.contains("Unknown map"));
        assertFalse(text.contains("message:"));
    }

    @Test
    void mapDeleteRemoves() throws Exception {
        MapManager.get().createMap("arena", "game", "Arena", "world");
        CommandSender sender = mock(CommandSender.class);

        invoke("mapDelete", ctx(null, "arena", stack(sender, null)));

        assertNull(MapManager.get().getMap("arena"));
    }

    @Test
    void mapDeleteUnknownMap() throws Exception {
        CommandSender sender = mock(CommandSender.class);

        invoke("mapDelete", ctx(null, "missing", stack(sender, null)));

        org.mockito.ArgumentCaptor<Component> sent = org.mockito.ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(sent.capture());
        String text = plainText(sent.getValue());
        assertTrue(text.contains("Unknown map"));
        assertFalse(text.contains("message:"));
    }

    @Test
    void databaseFailurePathsReportToSender() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        PlayerMock player = server.addPlayer();
        // Break the database: every map write below hits the SQLException catch.
        java.lang.reflect.Field field =
                org.drappula.arcadeCore.database.Database.class.getDeclaredField("connection");
        field.setAccessible(true);
        ((java.sql.Connection) field.get(null)).close();

        invoke("mapAddSpawn", ctx(null, "arena", stack(player, player.getLocation())));
        CommandSender sender = mock(CommandSender.class);
        invoke("mapSetEnabled", ctx(null, "arena", stack(sender, null)), true);
        invoke("mapDelete", ctx(null, "arena", stack(sender, null)));
        invoke("mapCreate", ctx("game", "other", stack(sender, player.getLocation())));

        assertNotNull(player.nextComponentMessage());
    }

    @Test
    void dispatchesGuardedBranches() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        var dispatcher = new com.mojang.brigadier.CommandDispatcher<CommandSourceStack>();
        dispatcher.getRoot().addChild(MainCommand.get());

        CommandSender noperm = mock(CommandSender.class);
        assertThrows(com.mojang.brigadier.exceptions.CommandSyntaxException.class,
                () -> dispatcher.execute("arcade start game", stack(noperm, null)));

        CommandSender admin = mock(CommandSender.class);
        when(admin.hasPermission(anyString())).thenReturn(true);
        dispatcher.execute("arcade start game", stack(admin, null));
        verify(admin).sendRichMessage(eq("<red>No players are queued for that game."));

        dispatcher.execute("arcade map disable arena", stack(admin, null));
        assertFalse(MapManager.get().getMap("arena").isEnabled());
        dispatcher.execute("arcade map enable arena", stack(admin, null));
        assertTrue(MapManager.get().getMap("arena").isEnabled());
    }

    @Test
    void suggestionsListGamesAndMaps() throws Exception {        GameManager.get().registerGame(new FakeGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        var gamesMethod = MainCommand.class.getDeclaredMethod("suggestGames",
                CommandContext.class, SuggestionsBuilder.class);
        gamesMethod.setAccessible(true);
        var mapsMethod = MainCommand.class.getDeclaredMethod("suggestMaps",
                CommandContext.class, SuggestionsBuilder.class);
        mapsMethod.setAccessible(true);

        var games = ((java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>)
                gamesMethod.invoke(null, null, new SuggestionsBuilder("", 0))).get();
        var maps = ((java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>)
                mapsMethod.invoke(null, null, new SuggestionsBuilder("", 0))).get();

        assertTrue(games.getList().stream().anyMatch(s -> s.getText().equals("game")));
        assertTrue(maps.getList().stream().anyMatch(s -> s.getText().equals("arena")));
    }

    @Test
    void mapConfigSetPersistsCanonicalValue() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        PlayerMock player = server.addPlayer();

        invoke("mapConfigSet", configCtx("arena", "speed", "007", player));

        assertEquals("7", MapManager.get().getMap("arena").getConfigOverrides().get("speed"));
        String text = plainText(player.nextComponentMessage());
        assertTrue(text.contains("speed") && text.contains("7"));
        assertFalse(text.contains("message:"));
    }

    @Test
    void mapConfigSetUnknownMap() throws Exception {
        CommandSender sender = mock(CommandSender.class);

        invoke("mapConfigSet", configCtx("missing", "speed", "5", sender));

        org.mockito.ArgumentCaptor<Component> sent = org.mockito.ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(sent.capture());
        assertTrue(plainText(sent.getValue()).contains("Unknown map"));
    }

    @Test
    void mapConfigSetUnknownKeyListsRegistered() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        CommandSender sender = mock(CommandSender.class);

        invoke("mapConfigSet", configCtx("arena", "nope", "5", sender));

        assertEquals("5", MapManager.get().getMap("arena").getConfigOverrides().get("speed"));
        org.mockito.ArgumentCaptor<Component> sent = org.mockito.ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(sent.capture());
        String text = plainText(sent.getValue());
        assertTrue(text.contains("nope") && text.contains("speed"));
    }

    @Test
    void mapConfigSetInvalidValueRejected() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        CommandSender sender = mock(CommandSender.class);

        invoke("mapConfigSet", configCtx("arena", "speed", "999", sender));
        invoke("mapConfigSet", configCtx("arena", "speed", "fast", sender));

        assertEquals("5", MapManager.get().getMap("arena").getConfigOverrides().get("speed"));
        org.mockito.ArgumentCaptor<Component> sent = org.mockito.ArgumentCaptor.forClass(Component.class);
        verify(sender, times(2)).sendMessage(sent.capture());
        assertTrue(plainText(sent.getAllValues().get(0)).contains("Invalid value"));
    }

    @Test
    void mapConfigSetRejectsWhenGameHasNoOptions() throws Exception {
        GameManager.get().registerGame(new FakeGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        CommandSender sender = mock(CommandSender.class);

        invoke("mapConfigSet", configCtx("arena", "speed", "5", sender));

        assertTrue(MapManager.get().getMap("arena").getConfigOverrides().isEmpty());
    }

    @Test
    void mapConfigUnsetRemovesOverride() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        MapManager.get().setMapConfig("arena", "speed", "42");
        PlayerMock player = server.addPlayer();

        invoke("mapConfigUnset", configCtx("arena", "speed", null, player));

        assertFalse(MapManager.get().getMap("arena").getConfigOverrides().containsKey("speed"));
        assertEquals("hello", MapManager.get().getMap("arena").getConfigOverrides().get("motd"));
        assertTrue(plainText(player.nextComponentMessage()).contains("speed"));
    }

    @Test
    void mapConfigUnsetWithoutOverrideIsInfo() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        org.drappula.arcadeCore.database.MapDataManager.deleteConfig("arena", "speed");
        MapManager.get().getMap("arena").getConfigInternal().remove("speed");
        CommandSender sender = mock(CommandSender.class);

        invoke("mapConfigUnset", configCtx("arena", "speed", null, sender));

        org.mockito.ArgumentCaptor<Component> sent = org.mockito.ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(sent.capture());
        assertTrue(plainText(sent.getValue()).contains("no override"));
    }

    @Test
    void mapConfigListShowsEffectiveValues() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        MapManager.get().setMapConfig("arena", "speed", "42");
        CommandSender sender = mock(CommandSender.class);

        invoke("mapConfigList", configCtx("arena", null, null, sender));

        verify(sender).sendRichMessage(argThat(message ->
                message.contains("speed=42") && message.contains("motd=hello")), any());
    }

    @Test
    void configKeysSuggestedForMapGame() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        var method = MainCommand.class.getDeclaredMethod("suggestConfigKeys",
                CommandContext.class, SuggestionsBuilder.class);
        method.setAccessible(true);
        CommandContext<CommandSourceStack> ctx = configCtx("arena", null, null, mock(CommandSender.class));

        var suggestions = ((java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>)
                method.invoke(null, ctx, new SuggestionsBuilder("", 0))).get();

        assertTrue(suggestions.getList().stream().anyMatch(s -> s.getText().equals("speed")));
        assertTrue(suggestions.getList().stream().anyMatch(s -> s.getText().equals("motd")));
    }

    @Test
    void dispatchesConfigBranches() throws Exception {
        GameManager.get().registerGame(new FakeOptionGame());
        MapManager.get().createMap("arena", "game", "Arena", "world");
        var dispatcher = new com.mojang.brigadier.CommandDispatcher<CommandSourceStack>();
        dispatcher.getRoot().addChild(MainCommand.get());
        CommandSender admin = mock(CommandSender.class);
        when(admin.hasPermission(anyString())).thenReturn(true);

        dispatcher.execute("arcade map config arena set speed 42", stack(admin, null));

        assertEquals("42", MapManager.get().getMap("arena").getConfigOverrides().get("speed"));
        dispatcher.execute("arcade map config arena list", stack(admin, null));
        dispatcher.execute("arcade map config arena unset speed", stack(admin, null));
        assertFalse(MapManager.get().getMap("arena").getConfigOverrides().containsKey("speed"));
    }
}
