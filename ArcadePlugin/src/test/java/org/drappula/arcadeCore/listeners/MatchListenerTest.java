package org.drappula.arcadeCore.listeners;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.game.MatchState;
import org.drappula.arcadeApi.systems.game.settings.MatchStartSettings;
import org.drappula.arcadeCore.ServerTest;
import org.drappula.arcadeCore.managers.game.GameManager;
import org.drappula.arcadeCore.managers.game.Match;
import org.drappula.arcadeCore.managers.game.MatchManager;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MatchListenerTest extends ServerTest {

    private static Game game(MatchStartSettings settings) {
        Game game = mock(Game.class);
        org.mockito.Mockito.when(game.getId()).thenReturn("game");
        org.mockito.Mockito.when(game.getMatchStartSettings()).thenReturn(settings);
        org.mockito.Mockito.when(game.getGameEndSettings()).thenReturn(
                org.drappula.arcadeApi.systems.game.settings.GameEndSettings.DEFAULT);
        return game;
    }

    private static PlayerMoveEvent move(PlayerMock player, int toX) {
        Location from = new Location(player.getWorld(), 0, 64, 0);
        return new PlayerMoveEvent(player, from, new Location(player.getWorld(), toX, 64, 0));
    }

    private final MatchListener listener = new MatchListener();

    private Match startingMatch(Game game, PlayerMock player) {
        Match match = new Match(game, new ArrayList<>(List.of(player)), null);
        GameManager.get().populateMatch(match);
        MatchManager.get().setState(match, MatchState.STARTING);
        return match;
    }

    @Test
    void movementFrozenDuringCountdownByDefault() {
        PlayerMock player = server.addPlayer();
        startingMatch(game(MatchStartSettings.DEFAULT), player);

        PlayerMoveEvent event = move(player, 1);
        listener.onMove(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void sameBlockMoveAllowed() {
        PlayerMock player = server.addPlayer();
        startingMatch(game(MatchStartSettings.DEFAULT), player);

        Location at = new Location(player.getWorld(), 0, 64, 0);
        PlayerMoveEvent event = new PlayerMoveEvent(player, at, at.clone().add(0.2, 0, 0));
        listener.onMove(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void freezeDisabledByGame() {
        PlayerMock player = server.addPlayer();
        startingMatch(game(MatchStartSettings.create(true, false)), player);

        PlayerMoveEvent event = move(player, 1);
        listener.onMove(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void startedMatchAllowsMovement() {
        PlayerMock player = server.addPlayer();
        Match match = startingMatch(game(MatchStartSettings.DEFAULT), player);
        MatchManager.get().setState(match, MatchState.STARTED);

        PlayerMoveEvent event = move(player, 1);
        listener.onMove(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void lobbyPlayerUnaffected() {
        PlayerMock player = server.addPlayer();

        PlayerMoveEvent event = move(player, 1);
        listener.onMove(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void eliminatedSpectatorUnaffected() {
        PlayerMock player = server.addPlayer();
        PlayerMock other = server.addPlayer();
        Match match = new Match(game(MatchStartSettings.DEFAULT),
                new ArrayList<>(List.of(player, other)), null);
        GameManager.get().populateMatch(match);
        MatchManager.get().setState(match, MatchState.STARTING);
        MatchManager.get().eliminateParticipant(match.getParticipants().get(0));

        PlayerMoveEvent event = move(player, 1);
        listener.onMove(event);

        assertFalse(event.isCancelled());
    }
}
