package org.drappula.arcadeCore.api;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeCore.ServerTest;
import org.drappula.arcadeCore.managers.game.GameManager;
import org.drappula.arcadeCore.managers.game.Match;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArcadeAPIImplTest extends ServerTest {

    private static class FakeGame implements Game {
        private final String id;

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
    }

    private static Player player(UUID uuid) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        return player;
    }

    private final ArcadeAPIImpl api = new ArcadeAPIImpl();

    @Test
    void findsParticipantByUuidAcrossPlayerInstances() {
        FakeGame game = new FakeGame("game");
        GameManager.get().registerGame(game);
        UUID uuid = UUID.randomUUID();
        Match match = new Match(game, List.of(player(uuid)), null);
        GameManager.get().populateMatch(match);

        // A different Player object for the same online player must still resolve.
        assertNotNull(api.getParticipant(game, player(uuid)));
    }

    @Test
    void unknownPlayerResolvesToNull() {
        FakeGame game = new FakeGame("game");
        GameManager.get().registerGame(game);
        Match match = new Match(game, List.of(player(UUID.randomUUID())), null);
        GameManager.get().populateMatch(match);

        assertNull(api.getParticipant(game, player(UUID.randomUUID())));
    }

    @Test
    void wrongGameResolvesToNull() {
        FakeGame game = new FakeGame("game");
        FakeGame other = new FakeGame("other");
        GameManager.get().registerGame(game);
        GameManager.get().registerGame(other);
        UUID uuid = UUID.randomUUID();
        GameManager.get().populateMatch(new Match(game, List.of(player(uuid)), null));

        assertNull(api.getParticipant(other, player(uuid)));
    }

    @Test
    void profileDelegatesToProfileManager() {
        Player player = player(UUID.randomUUID());

        assertNull(api.getProfile(player));
        assertFalse(api.isInMatch(player));
        assertFalse(api.isQueued(player));
    }

    @Test
    void deprecatedGlobalLookupFindsParticipant() {
        FakeGame game = new FakeGame("game");
        GameManager.get().registerGame(game);
        UUID uuid = UUID.randomUUID();
        GameManager.get().populateMatch(new Match(game, List.of(player(uuid)), null));

        assertNotNull(api.getParticipant(player(uuid)));
        assertNull(api.getParticipant(player(UUID.randomUUID())));
    }

    @Test
    void userDataDelegatesToStore() throws Exception {
        UUID uuid = UUID.randomUUID();

        api.getOrCreateUserData(uuid, "name");
        assertEquals("name", api.getUserData(uuid).orElseThrow().getUsername());

        api.saveUserData(new org.drappula.arcadeApi.database.UserData(uuid, "renamed"));
        assertEquals("renamed", api.getUserData(uuid).orElseThrow().getUsername());

        assertTrue(api.getUserData(UUID.randomUUID()).isEmpty());
    }

    @Test
    void sendToLobbyUsesPlayerService() {
        org.mockbukkit.mockbukkit.entity.PlayerMock player = server.addPlayer();

        api.sendToLobby(player);

        assertEquals(org.bukkit.GameMode.ADVENTURE, player.getGameMode());
    }
}
