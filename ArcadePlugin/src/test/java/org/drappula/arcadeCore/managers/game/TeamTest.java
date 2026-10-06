package org.drappula.arcadeCore.managers.game;

import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamTest {

    private static IParticipant alive() {
        IParticipant participant = mock(IParticipant.class);
        when(participant.isEliminated()).thenReturn(false);
        return participant;
    }

    private static IParticipant eliminated() {
        IParticipant participant = mock(IParticipant.class);
        when(participant.isEliminated()).thenReturn(true);
        return participant;
    }

    @Test
    void membersAreReturnedInOrder() {
        IMatch match = mock(IMatch.class);
        Team team = new Team("team-1", "Team 1", match);
        IParticipant first = alive();
        IParticipant second = alive();
        team.addMember(first);
        team.addMember(second);

        assertEquals(List.of(first, second), team.getMembers());
        assertEquals("team-1", team.getId());
        assertEquals("Team 1", team.getName());
        assertSame(match, team.getMatch());
        assertEquals(2, team.getSize());
    }

    @Test
    void aliveMembersSkipEliminated() {
        Team team = new Team("team-1", "Team 1", null);
        team.addMember(alive());
        team.addMember(eliminated());

        assertEquals(1, team.getAliveMembers().size());
        assertEquals(1, team.getAliveCount());
        assertFalse(team.isEliminated());
    }

    @Test
    void fullyEliminatedTeamReportsEliminated() {
        Team team = new Team("team-1", "Team 1", null);
        team.addMember(eliminated());

        assertTrue(team.isEliminated());
    }

    @Test
    void memberListIsDefensiveCopy() {
        Team team = new Team("team-1", "Team 1", null);
        team.addMember(alive());

        assertThrows(UnsupportedOperationException.class, () -> team.getMembers().add(alive()));
    }
}
