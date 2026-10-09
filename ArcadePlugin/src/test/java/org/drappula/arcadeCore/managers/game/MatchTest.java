package org.drappula.arcadeCore.managers.game;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.game.ITeam;
import org.drappula.arcadeApi.systems.game.MatchState;
import org.drappula.arcadeApi.systems.game.settings.TeamSettings;
import org.drappula.arcadeCore.ServerTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MatchTest extends ServerTest {

    private static Player player() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("player");
        return player;
    }

    private static List<Player> players(int count) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < count; i++) players.add(player());
        return players;
    }

    private static Game game(TeamSettings teams) {
        return new Game() {
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
            public boolean isTeamBased() {
                return teams.isEnabled();
            }

            @Override
            public TeamSettings getTeamSettings() {
                return teams;
            }
        };
    }

    @Test
    void soloMatchCreatesNoTeams() {
        Match match = new Match(game(TeamSettings.DISABLED), players(4), null);

        assertTrue(match.getTeams().isEmpty());
        assertEquals(4, match.getAliveParticipants().size());
        assertTrue(match.getEliminatedParticipants().isEmpty());
        assertEquals(MatchState.LOADING, match.getState());
        assertNotNull(match.getId());
        assertNotNull(match.getStartedAt());
    }

    @Test
    void teamGamePartitionsIntoFixedSizes() {
        Match match = new Match(game(TeamSettings.of(2, 4)), players(6), null);

        List<ITeam> teams = match.getTeams();
        assertEquals(3, teams.size());
        for (ITeam team : teams) assertEquals(2, team.getMembers().size());
    }

    @Test
    void overflowPlayersJoinLastTeamInsteadOfCreatingMore() {
        // 2 per team, at most 2 teams, 6 players: the last 2 overflow into team 2.
        Match match = new Match(game(TeamSettings.of(2, 2)), players(6), null);

        List<ITeam> teams = match.getTeams();
        assertEquals(2, teams.size());
        assertEquals(2, teams.get(0).getMembers().size());
        assertEquals(4, teams.get(1).getMembers().size());
    }

    @Test
    void everyParticipantBelongsToExactlyOneTeam() {
        Match match = new Match(game(TeamSettings.of(3, 3)), players(7), null);

        long assigned = match.getTeams().stream().mapToLong(t -> t.getMembers().size()).sum();
        assertEquals(7, assigned);
        for (var participant : match.getAliveParticipants()) {
            assertNotNull(participant.getTeam());
        }
    }

    @Test
    void soloParticipantsHaveNoTeam() {
        Match match = new Match(game(TeamSettings.DISABLED), players(2), null);

        for (var participant : match.getAliveParticipants()) {
            assertNull(participant.getTeam());
        }
    }

    @Test
    void participantListIsDefensiveCopy() {
        Match match = new Match(game(TeamSettings.DISABLED), players(2), null);

        assertThrows(UnsupportedOperationException.class, () -> match.getParticipants().add(null));
        assertThrows(UnsupportedOperationException.class, () -> match.getAliveParticipants().add(null));
        assertThrows(UnsupportedOperationException.class, () -> match.getEliminatedParticipants().add(null));
        assertThrows(UnsupportedOperationException.class, () -> match.getWinnerParticipants().add(null));
    }

    @Test
    void winnersAreCopiedOnSet() {
        Match match = new Match(game(TeamSettings.DISABLED), players(2), null);
        List<org.drappula.arcadeApi.systems.game.IParticipant> winners =
                new ArrayList<>(match.getAliveParticipants().subList(0, 1));

        match.setWinnerParticipants(winners);
        winners.clear();

        assertEquals(1, match.getWinnerParticipants().size());
    }

    @Test
    void spectatorsDeduplicate() {
        Match match = new Match(game(TeamSettings.DISABLED), players(1), null);
        Player spectator = player();

        match.addSpectator(spectator);
        match.addSpectator(spectator);

        assertEquals(1, match.getSpectatingPlayers().size());
        assertTrue(match.isSpectating(spectator));

        match.removeSpectator(spectator);
        assertFalse(match.isSpectating(spectator));
    }

    @Test
    void broadcastReachesParticipantsAndSpectators() {
        Player first = mock(Player.class);
        Player second = mock(Player.class);
        Player spectator = mock(Player.class);
        Match match = new Match(game(TeamSettings.DISABLED), List.of(first, second), null);
        match.addSpectator(spectator);

        match.broadcast("message:hello");

        verify(first, times(1)).sendMessage("hello");
        verify(second, times(1)).sendMessage("hello");
        verify(spectator, times(1)).sendMessage("hello");
    }

    @Test
    void endHandsOffToMatchManager() {
        Match match = new Match(game(TeamSettings.DISABLED), List.of(player(), player()), null);
        org.drappula.arcadeCore.managers.game.GameManager.get().registerGame(match.getGame());
        org.drappula.arcadeCore.managers.game.GameManager.get().populateMatch(match);

        match.end();

        assertEquals(MatchState.ENDING, match.getState());
        server.getScheduler().performTicks(6 * 20 + 40);
        assertEquals(MatchState.ENDED, match.getState());
    }
}
