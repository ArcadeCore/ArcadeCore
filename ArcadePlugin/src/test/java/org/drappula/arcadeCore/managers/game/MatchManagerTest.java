package org.drappula.arcadeCore.managers.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.database.GameStats;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.MatchState;
import org.drappula.arcadeApi.systems.game.settings.MatchStartSettings;
import org.drappula.arcadeApi.systems.map.IArcadeMap;
import org.drappula.arcadeCore.ServerTest;
import org.drappula.arcadeCore.database.GameStatsManager;
import org.drappula.arcadeCore.managers.map.MapManager;
import org.drappula.arcadeCore.managers.world.ArenaWorldManager;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchManagerTest extends ServerTest {
    private static class FakeGame implements Game {
        private final String id;
        private final boolean enabled;
        private IArcadeMap arena;
        boolean matchStarted;
        boolean matchEnded;
        MatchStartSettings settings = MatchStartSettings.DEFAULT;

        FakeGame(String id, boolean enabled) {
            this.id = id;
            this.enabled = enabled;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getDisplayName() {
            return id;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public int getPlayersRequired() {
            return 1;
        }

        @Override
        public IArcadeMap createArena(World world, List<Player> players) {
            return arena;
        }

        @Override
        public void onMatchStart(IMatch match) {
            matchStarted = true;
        }

        @Override
        public void onMatchEnd(IMatch match) {
            matchEnded = true;
        }

        @Override
        public MatchStartSettings getMatchStartSettings() {
            return settings;
        }
    }

    private static class FakeMap implements IArcadeMap {
        private final List<Location> spawns;
        private final org.bukkit.World world;

        FakeMap(List<Location> spawns) {
            this(spawns, null);
        }

        FakeMap(List<Location> spawns, org.bukkit.World world) {
            this.spawns = spawns;
            this.world = world;
        }

        @Override
        public String getId() {
            return "arena";
        }

        @Override
        public String getGameId() {
            return "game";
        }

        @Override
        public String getDisplayName() {
            return "Arena";
        }

        @Override
        public String getWorldName() {
            return "world";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public boolean isInUse() {
            return false;
        }

        @Override
        public List<Location> getSpawnPoints() {
            return spawns;
        }

        @Override
        public org.bukkit.World getWorld() {
            return world;
        }
    }

    private static class WorldCapturingGame extends FakeGame {
        World seen;

        WorldCapturingGame() {
            super("capture", true);
        }

        @Override
        public IArcadeMap createArena(World world, List<Player> players) {
            seen = world;
            List<Location> spawns = new ArrayList<>();
            for (Player player : players) {
                spawns.add(new Location(world, 0, 64, 0));
            }
            return new FakeMap(spawns, world);
        }
    }

    private org.bukkit.World world() {
        return server.addSimpleWorld("world");
    }

    @Test
    void emptyReads() {
        FakeGame game = new FakeGame("game", true);
        GameManager.get().registerGame(game);

        assertTrue(MatchManager.get().getMatches().isEmpty());
        assertTrue(MatchManager.get().getMatchesForGame(game).isEmpty());
        assertTrue(MatchManager.get().getMatchesForGame("game").isEmpty());
        assertTrue(MatchManager.get().getAllMatches().isEmpty());
        assertTrue(MatchManager.get().getMatch(server.addPlayer()).isEmpty());
        assertTrue(MatchManager.get().getMatchById(java.util.UUID.randomUUID()).isEmpty());
    }

    @Test
    void strangerIsInNoMatch() {
        FakeGame game = new FakeGame("game", true);
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();
        GameManager.get().populateMatch(new Match(game, new ArrayList<>(List.of(player)), null));

        assertTrue(MatchManager.get().getMatch(server.addPlayer()).isEmpty());
        assertTrue(MatchManager.get().getMatchById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void scanContinuesAcrossMatches() {
        FakeGame game = new FakeGame("game", true);
        GameManager.get().registerGame(game);
        PlayerMock other = server.addPlayer();
        PlayerMock wanted = server.addPlayer();
        GameManager.get().populateMatch(new Match(game, new ArrayList<>(List.of(other)), null));
        Match second = new Match(game, new ArrayList<>(List.of(wanted)), null);
        GameManager.get().populateMatch(second);

        assertEquals(java.util.Optional.of(second), MatchManager.get().getMatch(wanted));
        assertEquals(java.util.Optional.of(second), MatchManager.get().getMatchById(second.getId()));
    }

    @Test
    void lookupsFindParticipantsSpectatorsAndIds() {
        FakeGame game = new FakeGame("Game", true);
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();
        PlayerMock spectator = server.addPlayer();
        Match match = new Match(game, new ArrayList<>(List.of(player)), null);
        match.addSpectator(spectator);
        GameManager.get().populateMatch(match);

        assertEquals(List.of(match), MatchManager.get().getMatchesForGame("GAME"));
        assertEquals(List.of(match), MatchManager.get().getMatchesForGame(game));
        assertEquals(java.util.Optional.of(match), MatchManager.get().getMatchById(match.getId()));
        assertEquals(java.util.Optional.of(match), MatchManager.get().getMatch(spectator));
    }

    @Test
    void disabledGameThrows() {
        FakeGame game = new FakeGame("game", false);

        assertThrows(IllegalStateException.class,
                () -> MatchManager.get().startMatch(game, List.of(server.addPlayer())));
    }

    @Test
    void missingMapReturnsNull() {
        FakeGame game = new FakeGame("game", true);

        assertNull(MatchManager.get().startMatch(game, List.of(server.addPlayer())));
    }

    @Test
    void tooFewSpawnsReturnsNull() {
        FakeGame game = new FakeGame("game", true);
        game.arena = new FakeMap(List.of(new Location(world(), 0, 64, 0)));

        List<Player> players = List.of(server.addPlayer(), server.addPlayer());
        assertNull(MatchManager.get().startMatch(game, new ArrayList<>(players)));
    }

    @Test
    void startMatchHandsTempWorldToGame() {
        WorldCapturingGame game = new WorldCapturingGame();
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();

        Match match = MatchManager.get().startMatch(game, new ArrayList<>(List.of(player)));

        assertNotNull(match);
        assertNotNull(game.seen);
        assertTrue(game.seen.getName().startsWith("arcade-capture-"));
        assertSame(game.seen, match.getMap().getWorld());
        assertTrue(ArenaWorldManager.get().isArenaWorld(game.seen));
    }

    @Test
    void failedStartDestroysTempWorld() throws Exception {
        FakeGame game = new FakeGame("game", true);
        GameManager.get().registerGame(game);

        assertNull(MatchManager.get().startMatch(game, new ArrayList<>(List.of(server.addPlayer()))));

        assertTrue(trackedArenaWorlds().isEmpty());
    }

    @Test
    void arenaWorldDestroyedAfterTeardown() {
        WorldCapturingGame game = new WorldCapturingGame();
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();

        Match match = MatchManager.get().startMatch(game, new ArrayList<>(List.of(player)));
        assertNotNull(match);
        World arenaWorld = game.seen;

        match.end();
        server.getScheduler().performTicks(6 * 20 + 20);

        assertEquals(MatchState.ENDED, match.getState());
        assertFalse(ArenaWorldManager.get().isArenaWorld(arenaWorld));
    }

    private static String plainText(Component message) {
        return PlainTextComponentSerializer.plainText().serialize(message);
    }

    private static java.util.Set<?> trackedArenaWorlds() throws Exception {
        Field field = ArenaWorldManager.class.getDeclaredField("arenaWorlds");
        field.setAccessible(true);
        return (java.util.Set<?>) field.get(ArenaWorldManager.get());
    }

    @Test
    void fullLifecycleFromStartToTeardown() throws Exception {
        FakeGame game = new FakeGame("game", true);
        game.arena = new FakeMap(List.of(new Location(world(), 0, 64, 0), new Location(world(), 10, 64, 10)));
        GameManager.get().registerGame(game);
        PlayerMock first = server.addPlayer();
        PlayerMock second = server.addPlayer();

        IMatch match = MatchManager.get().startMatch(game, new ArrayList<>(List.of(first, second)));

        assertNotNull(match);
        assertEquals(MatchState.STARTING, match.getState());
        // Teleported to arena spawns.
        assertEquals(0, first.getLocation().getX());
        assertEquals(10, second.getLocation().getX());
        assertEquals(Optional.of(match), MatchManager.get().getMatch(first));

        // Start countdown (10s from config) elapses.
        server.getScheduler().performTicks(11 * 20 + 20);
        assertEquals(MatchState.STARTED, match.getState());
        assertTrue(game.matchStarted);

        // Declare a winner, then end: teardown records stats and lobbies everyone.
        match.setWinnerParticipants(List.of(match.getAliveParticipants().get(0)));
        MatchManager.get().endMatch(match);
        server.getScheduler().performTicks(6 * 20 + 20);

        assertEquals(MatchState.ENDED, match.getState());
        assertTrue(game.matchEnded);
        assertTrue(MatchManager.get().getAllMatches().isEmpty());
        assertEquals(GameMode.ADVENTURE, first.getGameMode());
        Optional<GameStats> stats = GameStatsManager.get().getStats(first.getUniqueId(), "game");
        assertTrue(stats.isPresent());
        assertEquals(1, stats.get().getWins());
    }

    @Test
    void endMatchAnnouncesWinnersImmediately() {
        FakeGame game = new FakeGame("game", true);
        GameManager.get().registerGame(game);
        PlayerMock first = server.addPlayer();
        PlayerMock second = server.addPlayer();
        Match match = new Match(game, new ArrayList<>(List.of(first, second)), null);
        GameManager.get().populateMatch(match);
        match.setWinnerParticipants(List.of(match.getAliveParticipants().get(0)));

        MatchManager.get().endMatch(match);

        // Winner announcement plus a chat line that the game ended, both
        // synchronous with end() (titles are a MockBukkit no-op).
        List<String> firstTexts = drainChat(first);
        List<String> secondTexts = drainChat(second);
        assertTrue(firstTexts.stream().anyMatch(text -> text.contains("won")));
        assertTrue(secondTexts.stream().anyMatch(text -> text.contains("won")));
        assertTrue(firstTexts.stream().anyMatch(text -> text.contains("ended")));
        assertTrue(firstTexts.stream().anyMatch(text -> text.contains(first.getName())));
    }

    private static List<String> drainChat(PlayerMock player) {
        List<String> texts = new ArrayList<>();
        // nextComponentMessage throws AssertionError once the queue is drained.
        for (int i = 0; i < 20; i++) {
            Component message;
            try {
                message = player.nextComponentMessage();
            } catch (AssertionError drained) {
                break;
            }
            if (message == null) break;
            texts.add(plainText(message));
        }
        return texts;
    }

    @Test
    void endMatchAnnouncesEndedTitleImmediately() {
        FakeGame game = new FakeGame("game", true);
        GameManager.get().registerGame(game);
        Player first = mock(Player.class);
        when(first.getUniqueId()).thenReturn(UUID.randomUUID());
        Player second = mock(Player.class);
        when(second.getUniqueId()).thenReturn(UUID.randomUUID());
        Match match = new Match(game, new ArrayList<>(List.of(first, second)), null);
        GameManager.get().populateMatch(match);

        org.drappula.arcadeApi.message.ScreenText screen = mock(org.drappula.arcadeApi.message.ScreenText.class);
        org.drappula.arcadeApi.message.Messages.useScreenText(screen);
        try {
            // No scheduler ticks: the title must fire synchronously with end().
            MatchManager.get().endMatch(match);

            verify(screen).title(eq(first), anyString(), any(), anyInt(), anyInt(), anyInt());
            verify(screen).title(eq(second), anyString(), any(), anyInt(), anyInt(), anyInt());
        } finally {
            org.drappula.arcadeApi.message.Messages.useScreenText(null);
        }
    }

    @Test
    void endMatchWithoutWinnersStaysSilent() {        FakeGame game = new FakeGame("game", true);
        GameManager.get().registerGame(game);
        PlayerMock first = server.addPlayer();
        PlayerMock second = server.addPlayer();
        Match match = new Match(game, new ArrayList<>(List.of(first, second)), null);
        GameManager.get().populateMatch(match);

        MatchManager.get().endMatch(match);
        server.getScheduler().performTicks(6 * 20 + 20);

        assertNoWonAnnouncement(first);
        assertNoWonAnnouncement(second);
    }

    private static void assertNoWonAnnouncement(PlayerMock player) {
        // nextComponentMessage throws AssertionError once the queue is drained.
        for (int i = 0; i < 20; i++) {
            Component message;
            try {
                message = player.nextComponentMessage();
            } catch (AssertionError drained) {
                return;
            }
            if (message == null) return;
            assertFalse(plainText(message).contains("won"));
        }
        fail("Unexpected message flood while checking for winner announcements");
    }

    @Test
    void teardownClearsInventories() {
        FakeGame game = new FakeGame("game", true);
        game.arena = new FakeMap(List.of(new Location(world(), 0, 64, 0), new Location(world(), 10, 64, 10)));
        GameManager.get().registerGame(game);
        PlayerMock first = server.addPlayer();
        PlayerMock second = server.addPlayer();
        Match match = MatchManager.get().startMatch(game, new ArrayList<>(List.of(first, second)));
        assertNotNull(match);
        first.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE, 3));
        second.getInventory().setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_HELMET));

        MatchManager.get().endMatch(match);
        server.getScheduler().performTicks(6 * 20 + 20);

        assertTrue(first.getInventory().isEmpty());
        assertTrue(second.getInventory().isEmpty());
    }

    @Test
    void eliminateMovesToSpectatorsWithEndSettings() {
        FakeGame game = new FakeGame("game", true);
        GameManager.get().registerGame(game);
        PlayerMock first = server.addPlayer();
        PlayerMock second = server.addPlayer();
        Match match = new Match(game, new ArrayList<>(List.of(first, second)), null);
        GameManager.get().populateMatch(match);

        match.getAliveParticipants().get(0).eliminate();

        assertEquals(1, match.getAliveParticipants().size());
        assertEquals(1, match.getEliminatedParticipants().size());
        assertTrue(match.isSpectating(first));
        // match.end gamemode SPECTATOR + fly per bundled config.
        assertEquals(GameMode.SPECTATOR, first.getGameMode());
        assertTrue(first.getAllowFlight());
        assertNotNull(second.nextComponentMessage());
    }

    @Test
    void setStateFiresChangeEvent() {
        FakeGame game = new FakeGame("game", true);
        GameManager.get().registerGame(game);
        Match match = new Match(game, new ArrayList<>(List.of(server.addPlayer())), null);
        java.util.concurrent.atomic.AtomicReference<org.drappula.arcadeApi.events.MatchStateChangeEvent> seen =
                new java.util.concurrent.atomic.AtomicReference<>();
        server.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            void onChange(org.drappula.arcadeApi.events.MatchStateChangeEvent event) {
                seen.set(event);
            }
        }, org.mockbukkit.mockbukkit.MockBukkit.createMockPlugin());

        MatchManager.get().setState(match, MatchState.STARTED);

        assertEquals(MatchState.STARTED, match.getState());
        assertNotNull(seen.get());
        assertSame(MatchState.LOADING, seen.get().getOldState());
        assertSame(MatchState.STARTED, seen.get().getNewState());
    }

    @Test
    void setStateSameValueFiresNothing() {
        FakeGame game = new FakeGame("game", true);
        Match match = new Match(game, new ArrayList<>(List.of(server.addPlayer())), null);
        java.util.concurrent.atomic.AtomicBoolean fired = new java.util.concurrent.atomic.AtomicBoolean(false);
        server.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            void onChange(org.drappula.arcadeApi.events.MatchStateChangeEvent event) {
                fired.set(true);
            }
        }, org.mockbukkit.mockbukkit.MockBukkit.createMockPlugin());

        MatchManager.get().setState(match, MatchState.LOADING);

        assertFalse(fired.get());
    }

    @Test
    void cancelledStartReleasesAndReturnsNull() {
        FakeGame game = new FakeGame("game", true);
        game.arena = new FakeMap(List.of(new Location(world(), 0, 64, 0)));
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();
        server.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            void onStart(org.drappula.arcadeApi.events.MatchStartEvent event) {
                event.setCancelled(true);
            }
        }, org.mockbukkit.mockbukkit.MockBukkit.createMockPlugin());

        assertNull(MatchManager.get().startMatch(game, new ArrayList<>(List.of(player))));
        assertTrue(MatchManager.get().getAllMatches().isEmpty());
    }

    @Test
    void cancelledEndLeavesMatchRunning() {
        FakeGame game = new FakeGame("game", true);
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();
        Match match = new Match(game, new ArrayList<>(List.of(player)), null);
        match.setState(MatchState.STARTED);
        GameManager.get().populateMatch(match);
        server.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            void onEnd(org.drappula.arcadeApi.events.MatchEndEvent event) {
                event.setCancelled(true);
            }
        }, org.mockbukkit.mockbukkit.MockBukkit.createMockPlugin());

        MatchManager.get().endMatch(match);
        server.getScheduler().performTicks(200);

        assertEquals(MatchState.STARTED, match.getState());
        assertEquals(List.of(match), MatchManager.get().getAllMatches());
    }

    @Test
    void doubleEndDoesNotDoubleCountStats() throws Exception {
        FakeGame game = new FakeGame("game", true);
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();
        Match match = new Match(game, new ArrayList<>(List.of(player)), null);
        GameManager.get().populateMatch(match);
        match.setWinnerParticipants(List.of(match.getAliveParticipants().get(0)));

        MatchManager.get().endMatch(match);
        MatchManager.get().endMatch(match);
        server.getScheduler().performTicks(6 * 20 + 40);

        assertEquals(MatchState.ENDED, match.getState());
        assertEquals(1, GameStatsManager.get().getStats(player.getUniqueId(), "game").orElseThrow().getWins());
    }

    @Test
    void mapPoolMapIsReleasedOnFailedStart() throws Exception {
        FakeGame game = new FakeGame("game", true);
        GameManager.get().registerGame(game);
        // Pool map with one spawn for two players: start fails after acquiring.
        MapManager.get().createMap("arena", "game", "Arena", "world");
        MapManager.get().addSpawn("arena", new Location(world(), 0, 64, 0));

        List<Player> players = new ArrayList<>(List.of(server.addPlayer(), server.addPlayer()));
        assertNull(MatchManager.get().startMatch(game, players));

        assertFalse(MapManager.get().getMap("arena").isInUse());
    }

    @Test
    void endAfterEndedIsNoop() {        FakeGame game = new FakeGame("game", true);
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();
        Match match = new Match(game, new ArrayList<>(List.of(player)), null);
        GameManager.get().populateMatch(match);

        MatchManager.get().endMatch(match);
        server.getScheduler().performTicks(6 * 20 + 40);
        assertEquals(MatchState.ENDED, match.getState());

        MatchManager.get().endMatch(match);
        server.getScheduler().performTicks(200);

        assertEquals(MatchState.ENDED, match.getState());
        assertTrue(MatchManager.get().getAllMatches().isEmpty());
    }

    private static class CagedGame extends FakeGame {
        final World world;

        CagedGame(World world) {
            super("caged", true);
            this.world = world;
        }

        @Override
        public IArcadeMap createArena(World arenaWorld, List<Player> players) {
            List<Location> spawns = new ArrayList<>();
            for (int i = 0; i < players.size(); i++) {
                spawns.add(new Location(world, i * 10, 64, 0));
            }
            return new FakeMap(spawns, world);
        }
    }

    @Test
    void startMatchBuildsCagesByDefault() {
        World world = world();
        CagedGame game = new CagedGame(world);
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();

        Match match = MatchManager.get().startMatch(game, new ArrayList<>(List.of(player)));

        assertNotNull(match);
        assertEquals(MatchState.STARTING, match.getState());
        // Ring around the player's feet plus the cap above.
        assertEquals(org.bukkit.Material.GLASS, world.getBlockAt(1, 64, 0).getType());
        assertEquals(org.bukkit.Material.GLASS, world.getBlockAt(0, 65, 1).getType());
        assertEquals(org.bukkit.Material.GLASS, world.getBlockAt(0, 66, 0).getType());
        assertEquals(org.bukkit.Material.AIR, world.getBlockAt(0, 64, 0).getType());
    }

    @Test
    void cagesRestoreOriginalBlocksOnStart() {
        World world = world();
        world.getBlockAt(1, 64, 0).setType(org.bukkit.Material.STONE);
        CagedGame game = new CagedGame(world);
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();

        Match match = MatchManager.get().startMatch(game, new ArrayList<>(List.of(player)));
        assertNotNull(match);
        assertEquals(org.bukkit.Material.GLASS, world.getBlockAt(1, 64, 0).getType());

        server.getScheduler().performTicks(15 * 20);

        assertEquals(MatchState.STARTED, match.getState());
        assertEquals(org.bukkit.Material.STONE, world.getBlockAt(1, 64, 0).getType());
        assertEquals(org.bukkit.Material.AIR, world.getBlockAt(0, 66, 0).getType());
    }

    @Test
    void cagesDisabledByGame() {
        World world = world();
        CagedGame game = new CagedGame(world);
        game.settings = MatchStartSettings.create(false, true);
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();

        assertNotNull(MatchManager.get().startMatch(game, new ArrayList<>(List.of(player))));

        assertEquals(org.bukkit.Material.AIR, world.getBlockAt(1, 64, 0).getType());
        assertEquals(org.bukkit.Material.AIR, world.getBlockAt(0, 66, 0).getType());
    }

    @Test
    void endMatchDuringCountdownClearsCages() {
        World world = world();
        CagedGame game = new CagedGame(world);
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();

        Match match = MatchManager.get().startMatch(game, new ArrayList<>(List.of(player)));
        assertNotNull(match);
        assertEquals(org.bukkit.Material.GLASS, world.getBlockAt(1, 64, 0).getType());

        match.end();

        assertEquals(org.bukkit.Material.AIR, world.getBlockAt(1, 64, 0).getType());
    }
}
