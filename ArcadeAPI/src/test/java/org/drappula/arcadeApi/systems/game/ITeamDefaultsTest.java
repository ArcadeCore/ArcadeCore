package org.drappula.arcadeApi.systems.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ITeamDefaultsTest {

    private static ITeam team(IParticipant... members) {
        return new ITeam() {
            @Override
            public String getId() {
                return "team";
            }

            @Override
            public String getName() {
                return "Team";
            }

            @Override
            public IMatch getMatch() {
                return null;
            }

            @Override
            public List<IParticipant> getMembers() {
                return List.of(members);
            }

            @Override
            public List<IParticipant> getAliveMembers() {
                return List.of(members).stream().filter(m -> !m.isEliminated()).toList();
            }
        };
    }

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
    void sizeCountsAllMembers() {
        assertEquals(3, team(alive(), alive(), eliminated()).getSize());
    }

    @Test
    void aliveCountSkipsEliminated() {
        assertEquals(2, team(alive(), alive(), eliminated()).getAliveCount());
    }

    @Test
    void eliminatedOnlyWhenNobodyAlive() {
        assertTrue(team(eliminated(), eliminated()).isEliminated());
        assertFalse(team(alive(), eliminated()).isEliminated());
        // Vacuous truth: a team with no members has no alive members.
        assertTrue(team().isEliminated());
    }
}
