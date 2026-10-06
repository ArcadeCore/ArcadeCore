package org.drappula.arcadeApi.events;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EventsTest {

    @Test
    void matchEndIsCancellable() {
        IMatch match = mock(IMatch.class);
        MatchEndEvent event = new MatchEndEvent(match);

        assertSame(match, event.getMatch());
        assertFalse(event.isCancelled());

        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }

    @Test
    void matchStartIsCancellable() {
        IMatch match = mock(IMatch.class);
        MatchStartEvent event = new MatchStartEvent(match);

        assertSame(match, event.getMatch());
        assertFalse(event.isCancelled());

        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }

    @Test
    void participantEliminateCarriesKiller() {
        IParticipant participant = mock(IParticipant.class);
        Player killer = mock(Player.class);

        assertNull(new ParticipantEliminateEvent(participant).getKiller());
        assertSame(killer, new ParticipantEliminateEvent(participant, killer).getKiller());

        ParticipantEliminateEvent event = new ParticipantEliminateEvent(participant, killer);
        assertSame(participant, event.getParticipant());
        assertFalse(event.isCancelled());
        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }

    @Test
    void queueEnterIsCancellable() {
        Player player = mock(Player.class);
        Game game = mock(Game.class);
        QueueEnterEvent event = new QueueEnterEvent(player, game);

        assertSame(player, event.getPlayer());
        assertSame(game, event.getGame());
        assertFalse(event.isCancelled());
        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }
}
