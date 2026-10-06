package org.drappula.arcadeApi.systems.game;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.map.IArcadeMap;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IMatchDefaultsTest {

    /** Minimal controllable IMatch fake: only state + winners are mutable. */
    private static class FakeMatch implements IMatch {
        private MatchState state;
        private List<IParticipant> winners = new ArrayList<>();
        private boolean ended;

        FakeMatch(MatchState state) {
            this.state = state;
        }

        @Override
        public UUID getId() {
            return new UUID(0, 1);
        }

        @Override
        public MatchState getState() {
            return state;
        }

        @Override
        public void setState(MatchState state) {
            this.state = state;
        }

        @Override
        public Instant getStartedAt() {
            return Instant.EPOCH;
        }

        @Override
        public List<IParticipant> getParticipants() {
            return List.of();
        }

        @Override
        public List<IParticipant> getAliveParticipants() {
            return List.of();
        }

        @Override
        public List<IParticipant> getEliminatedParticipants() {
            return List.of();
        }

        @Override
        public List<IParticipant> getWinnerParticipants() {
            return winners;
        }

        @Override
        public void setWinnerParticipants(List<IParticipant> participants) {
            winners = participants;
        }

        @Override
        public List<Player> getSpectatingPlayers() {
            return List.of();
        }

        @Override
        public List<ITeam> getTeams() {
            return List.of();
        }

        @Override
        public Game getGame() {
            return null;
        }

        @Override
        public @Nullable IArcadeMap getMap() {
            return null;
        }

        @Override
        public void end() {
            ended = true;
        }

        @Override
        public void addSpectator(Player player) {
        }

        @Override
        public void removeSpectator(Player player) {
        }

        @Override
        public boolean isSpectating(Player player) {
            return false;
        }

        @Override
        public void broadcast(String miniMessage, TagResolver... resolvers) {
        }
    }

    @Test
    void runningOnlyWhileStartingOrStarted() {
        assertFalse(new FakeMatch(MatchState.LOADING).isRunning());
        assertTrue(new FakeMatch(MatchState.STARTING).isRunning());
        assertTrue(new FakeMatch(MatchState.STARTED).isRunning());
        assertFalse(new FakeMatch(MatchState.ENDING).isRunning());
        assertFalse(new FakeMatch(MatchState.ENDED).isRunning());
    }

    @Test
    void endWithWinnersSetsWinnersBeforeEnding() {
        FakeMatch match = new FakeMatch(MatchState.STARTED);
        List<IParticipant> winners = List.of();

        match.endWithWinners(winners);

        assertSame(winners, match.getWinnerParticipants());
        assertTrue(match.ended);
    }

    @Test
    void handlerListsAreSingleton() {
        FakeMatch match = new FakeMatch(MatchState.STARTED);
        org.bukkit.entity.Player player = org.mockito.Mockito.mock(org.bukkit.entity.Player.class);
        org.drappula.arcadeApi.systems.game.Game game =
                org.mockito.Mockito.mock(org.drappula.arcadeApi.systems.game.Game.class);

        assertNotNull(org.drappula.arcadeApi.events.MatchEndEvent.getHandlerList());
        assertNotNull(org.drappula.arcadeApi.events.MatchStartEvent.getHandlerList());
        assertNotNull(org.drappula.arcadeApi.events.MatchStateChangeEvent.getHandlerList());
        assertNotNull(org.drappula.arcadeApi.events.QueueEnterEvent.getHandlerList());
        assertNotNull(org.drappula.arcadeApi.events.QueueLeaveEvent.getHandlerList());
        assertNotNull(org.drappula.arcadeApi.events.ParticipantEliminateEvent.getHandlerList());

        assertSame(org.drappula.arcadeApi.events.MatchEndEvent.getHandlerList(),
                new org.drappula.arcadeApi.events.MatchEndEvent(match).getHandlers());
        assertSame(org.drappula.arcadeApi.events.MatchStartEvent.getHandlerList(),
                new org.drappula.arcadeApi.events.MatchStartEvent(match).getHandlers());
        assertSame(org.drappula.arcadeApi.events.MatchStateChangeEvent.getHandlerList(),
                new org.drappula.arcadeApi.events.MatchStateChangeEvent(
                        match, MatchState.LOADING, MatchState.STARTED).getHandlers());
        assertSame(org.drappula.arcadeApi.events.QueueEnterEvent.getHandlerList(),
                new org.drappula.arcadeApi.events.QueueEnterEvent(player, game).getHandlers());
        assertSame(org.drappula.arcadeApi.events.QueueLeaveEvent.getHandlerList(),
                new org.drappula.arcadeApi.events.QueueLeaveEvent(player, game,
                        org.drappula.arcadeApi.systems.queue.QueueLeaveReason.LEAVE).getHandlers());
        assertSame(org.drappula.arcadeApi.events.ParticipantEliminateEvent.getHandlerList(),
                new org.drappula.arcadeApi.events.ParticipantEliminateEvent(
                        org.mockito.Mockito.mock(
                                org.drappula.arcadeApi.systems.game.IParticipant.class)).getHandlers());
    }
}
