package org.drappula.arcadeCore.managers.game;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.drappula.arcadeApi.events.ParticipantEliminateEvent;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.game.MatchState;
import org.bukkit.entity.Player;
import org.drappula.arcadeCore.ServerTest;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ParticipantTest extends ServerTest {

    private static class FakeGame implements Game {
        @Override
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
    }

    private Match populated(Game game, Player... players) {
        GameManager.get().registerGame(game);
        Match match = new Match(game, new ArrayList<>(List.of(players)), null);
        GameManager.get().populateMatch(match);
        return match;
    }

    @Test
    void eliminateMovesToEliminatedAndSpectating() {
        FakeGame game = new FakeGame();
        PlayerMock player = server.addPlayer();
        Match match = populated(game, player, server.addPlayer());

        match.getAliveParticipants().get(0).eliminate();

        assertEquals(1, match.getAliveParticipants().size());
        assertEquals(1, match.getEliminatedParticipants().size());
        assertTrue(match.isSpectating(player));
        assertTrue(match.getEliminatedParticipants().get(0).isEliminated());
    }

    @Test
    void killerReachesEventListeners() {
        FakeGame game = new FakeGame();
        PlayerMock player = server.addPlayer();
        PlayerMock killer = server.addPlayer();
        Match match = populated(game, player);
        AtomicReference<Player> seen = new AtomicReference<>();
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            void onEliminate(ParticipantEliminateEvent event) {
                seen.set(event.getKiller());
            }
        }, MockBukkit.createMockPlugin());

        match.getAliveParticipants().get(0).eliminate(killer);

        assertSame(killer, seen.get());
    }

    @Test
    void cancelledEliminationChangesNothing() {
        FakeGame game = new FakeGame();
        PlayerMock player = server.addPlayer();
        Match match = populated(game, player);
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            void onEliminate(ParticipantEliminateEvent event) {
                event.setCancelled(true);
            }
        }, MockBukkit.createMockPlugin());

        match.getAliveParticipants().get(0).eliminate();

        assertEquals(1, match.getAliveParticipants().size());
        assertTrue(match.getEliminatedParticipants().isEmpty());
        assertFalse(match.getAliveParticipants().get(0).isEliminated());
    }

    @Test
    void eliminationDuringTeardownIsIgnored() {
        FakeGame game = new FakeGame();
        PlayerMock player = server.addPlayer();
        Match match = populated(game, player);
        match.setState(MatchState.ENDING);

        match.getAliveParticipants().get(0).eliminate();

        assertEquals(1, match.getAliveParticipants().size());
        assertFalse(match.isSpectating(player));
    }

    @Test
    void doubleEliminateIsNoop() {
        FakeGame game = new FakeGame();
        PlayerMock player = server.addPlayer();
        Match match = populated(game, player, server.addPlayer());

        var participant = match.getAliveParticipants().get(0);
        participant.eliminate();
        participant.eliminate();

        assertEquals(1, match.getAliveParticipants().size());
        assertEquals(1, match.getEliminatedParticipants().size());
    }

    @Test
    void eliminateWithoutPopulateCreatesRosterEntry() {
        FakeGame game = new FakeGame();
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();
        // Never populated: GameManager has no roster for this game yet.
        Match match = new Match(game, new ArrayList<>(List.of(player)), null);

        match.getAliveParticipants().get(0).eliminate();

        assertEquals(1, match.getEliminatedParticipants().size());
        assertTrue(GameManager.get().getParticipants().containsKey("game"));
    }

    @Test
    void profileAndUserDataResolve() {
        FakeGame game = new FakeGame();
        PlayerMock player = server.addPlayer();
        org.drappula.arcadeCore.managers.ProfileManager.registerProfile(
                new org.drappula.arcadeCore.managers.impl.Profile(player));
        try {
            Match match = populated(game, player);
            var participant = match.getAliveParticipants().get(0);

            assertSame(org.drappula.arcadeCore.managers.ProfileManager.getProfile(player),
                    participant.getProfile());
            assertEquals(player.getUniqueId(), participant.getUserData().getUuid());
        } finally {
            org.drappula.arcadeCore.managers.ProfileManager.removeProfile(player);
        }
    }
}
