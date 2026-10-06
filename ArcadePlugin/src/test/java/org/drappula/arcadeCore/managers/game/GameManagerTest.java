package org.drappula.arcadeCore.managers.game;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeCore.managers.ProfileManager;
import org.drappula.arcadeCore.managers.impl.Profile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameManagerTest {

    private static class FakeGame implements Game {
        private final String id;
        int registered;
        int unregistered;

        FakeGame(String id) {
            this.id = id;
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
        public int getPlayersRequired() {
            return 1;
        }

        @Override
        public void onRegister() {
            registered++;
        }

        @Override
        public void onUnregister() {
            unregistered++;
        }
    }

    private static Player player() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }

    private final List<Player> trackedPlayers = new ArrayList<>();

    private Player trackedPlayer() {
        Player player = player();
        trackedPlayers.add(player);
        ProfileManager.registerProfile(new Profile(player));
        return player;
    }

    @BeforeEach
    void isolate() {
        GameManager.get().reload();
    }

    @AfterEach
    void cleanProfiles() {
        for (Player player : trackedPlayers) ProfileManager.removeProfile(player);
        GameManager.get().reload();
    }

    @Test
    void registerNormalizesIdCase() {
        GameManager.get().registerGame(new FakeGame("TestGame"));

        assertNotNull(GameManager.get().getGame("testgame"));
        assertNotNull(GameManager.get().getGame("TESTGAME"));
        assertTrue(GameManager.get().isRegistered("testgame"));
        assertFalse(GameManager.get().isRegistered("other"));
        assertFalse(GameManager.get().isRegistered(null));
        assertNull(GameManager.get().getGame(null));
    }

    @Test
    void rawViewsReflectRegistration() {
        FakeGame game = new FakeGame("game");
        GameManager.get().registerGame(game);

        assertSame(game, GameManager.get().getGames().get("game"));
        assertTrue(GameManager.get().getMatches().isEmpty());
        assertTrue(GameManager.get().getParticipants().isEmpty());
    }

    @Test
    void registerRejectsInvalidIds() {
        assertThrows(IllegalArgumentException.class, () -> GameManager.get().registerGame(new FakeGame("")));
        assertThrows(IllegalArgumentException.class, () -> GameManager.get().registerGame(new FakeGame("   ")));
        assertThrows(IllegalArgumentException.class, () -> GameManager.get().registerGame(new FakeGame("has space")));
    }

    @Test
    void registerFiresOnRegister() {
        FakeGame game = new FakeGame("game");

        GameManager.get().registerGame(game);

        assertEquals(1, game.registered);
    }

    @Test
    void unregisterFiresOnUnregister() {
        FakeGame game = new FakeGame("game");
        GameManager.get().registerGame(game);

        GameManager.get().unregisterGame("GAME");

        assertEquals(1, game.unregistered);
        assertNull(GameManager.get().getGame("game"));
    }

    @Test
    void unregisterByInstanceDelegates() {
        FakeGame game = new FakeGame("game");
        GameManager.get().registerGame(game);

        GameManager.get().unregisterGame(game);

        assertEquals(1, game.unregistered);
        assertNull(GameManager.get().getGame("game"));
    }

    @Test
    void unregisterUnknownGameIsNoop() {
        assertDoesNotThrow(() -> GameManager.get().unregisterGame("missing"));
    }

    @Test
    void reloadFiresOnUnregisterAndClears() {
        FakeGame first = new FakeGame("first");
        FakeGame second = new FakeGame("second");
        GameManager.get().registerGame(first);
        GameManager.get().registerGame(second);

        GameManager.get().reload();

        assertEquals(1, first.unregistered);
        assertEquals(1, second.unregistered);
        assertTrue(GameManager.get().getRegisteredGames().isEmpty());
    }

    @Test
    void populateTracksParticipantsAndLinksProfiles() {
        FakeGame game = new FakeGame("game");
        GameManager.get().registerGame(game);
        Player player = trackedPlayer();
        Match match = new Match(game, List.of(player), null);

        GameManager.get().populateMatch(match);

        assertTrue(GameManager.get().getMatches().get("game").contains(match));
        assertEquals(1, GameManager.get().getParticipants().get("game").size());
        assertSame(match, ProfileManager.getProfile(player).getMatch());
    }

    @Test
    void depopulateRemovesParticipantsAndUnlinksProfiles() {
        FakeGame game = new FakeGame("game");
        GameManager.get().registerGame(game);
        Player player = trackedPlayer();
        Player spectator = trackedPlayer();
        Match match = new Match(game, List.of(player), null);
        match.addSpectator(spectator);
        GameManager.get().populateMatch(match);
        ProfileManager.getProfile(spectator).setMatch(match);

        GameManager.get().depopulateMatch(match);

        assertFalse(GameManager.get().getMatches().get("game").contains(match));
        assertTrue(GameManager.get().getParticipants().get("game").isEmpty());
        assertNull(ProfileManager.getProfile(player).getMatch());
        assertNull(ProfileManager.getProfile(spectator).getMatch());
    }

    @Test
    void depopulateWithoutPopulateIsSafe() {
        FakeGame game = new FakeGame("game");
        GameManager.get().registerGame(game);
        Player player = trackedPlayer();
        Match match = new Match(game, List.of(player), null);

        assertDoesNotThrow(() -> GameManager.get().depopulateMatch(match));
    }

    @Test
    void registerGameBackfillsMissingDefaults() throws Exception {
        org.drappula.arcadeCore.database.TestDb.connect();
        try {
            org.drappula.arcadeCore.managers.map.MapManager.get()
                    .createMap("backfill-arena", "backfill-game", "Arena", "world");
            org.drappula.arcadeCore.managers.map.MapManager.get()
                    .setMapConfig("backfill-arena", "speed", "42");
            Game game = mock(Game.class);
            when(game.getId()).thenReturn("backfill-game");
            when(game.getMapConfigOptions()).thenReturn(List.of(
                    org.drappula.arcadeApi.systems.map.MapConfigOption.integer("speed", 5, 0, 60),
                    org.drappula.arcadeApi.systems.map.MapConfigOption.text("motd", "hello")));

            GameManager.get().registerGame(game);

            assertEquals(java.util.Map.of("speed", "42", "motd", "hello"),
                    org.drappula.arcadeCore.managers.map.MapManager.get()
                            .getMap("backfill-arena").getConfigOverrides());
        } finally {
            GameManager.get().unregisterGame("backfill-game");
            try {
                org.drappula.arcadeCore.managers.map.MapManager.get().deleteMap("backfill-arena");
            } catch (Exception ignored) {
            }
            org.drappula.arcadeCore.database.TestDb.disconnect();
        }
    }
}
