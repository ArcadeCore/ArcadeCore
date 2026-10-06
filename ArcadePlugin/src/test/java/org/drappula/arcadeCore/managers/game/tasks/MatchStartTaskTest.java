package org.drappula.arcadeCore.managers.game.tasks;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.MatchState;
import org.drappula.arcadeCore.ServerTest;
import org.drappula.arcadeCore.managers.game.Match;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MatchStartTaskTest extends ServerTest {

    private static class FakeGame implements Game {
        boolean started;

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

        @Override
        public void onMatchStart(IMatch match) {
            started = true;
        }
    }

    @Test
    void countdownFinishesIntoStarted() {
        FakeGame game = new FakeGame();
        PlayerMock player = server.addPlayer();
        Match match = new Match(game, new ArrayList<>(List.of(player)), null);
        new MatchStartTask(match).runTaskTimer(core(), 0, 20);

        // Bundled config countdown is 10s: the 11th run starts the match.
        server.getScheduler().performTicks(10 * 20);
        assertEquals(MatchState.LOADING, match.getState());
        server.getScheduler().performTicks(20);

        assertEquals(MatchState.STARTED, match.getState());
        assertTrue(game.started);
    }

    @Test
    void countdownNotifiesOnlyAliveParticipants() {
        FakeGame game = new FakeGame();
        Player alive = mock(Player.class);
        Player eliminated = mock(Player.class);
        when(alive.getName()).thenReturn("alive");
        when(eliminated.getName()).thenReturn("eliminated");
        Match match = new Match(game, new ArrayList<>(List.of(alive, eliminated)), null);
        org.drappula.arcadeCore.managers.game.GameManager.get().registerGame(game);
        org.drappula.arcadeCore.managers.game.GameManager.get().populateMatch(match);
        match.getAliveParticipants().stream()
                .filter(p -> p.getPlayer() == eliminated)
                .findFirst().orElseThrow().eliminate();
        new MatchStartTask(match).runTaskTimer(core(), 0, 20);

        server.getScheduler().performTicks(20);

        verify(alive, times(1)).sendTitlePart(
                eq(net.kyori.adventure.title.TitlePart.TITLE), any());
        verify(eliminated, never()).sendTitlePart(
                eq(net.kyori.adventure.title.TitlePart.TITLE), any());
        org.drappula.arcadeCore.managers.game.GameManager.get().depopulateMatch(match);
    }
}
