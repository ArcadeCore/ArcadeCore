package org.drappula.arcadeCore.managers.queue;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.drappula.arcadeApi.events.QueueEnterEvent;
import org.drappula.arcadeApi.events.QueueLeaveEvent;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.queue.JoinResult;
import org.drappula.arcadeApi.systems.queue.QueueLeaveReason;
import org.drappula.arcadeCore.ServerTest;
import org.drappula.arcadeCore.managers.game.GameManager;
import org.drappula.arcadeCore.managers.game.Match;
import org.drappula.arcadeCore.managers.game.MatchManager;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueueManagerTest extends ServerTest {

    private static class FakeGame implements Game {
        private final String id;
        private final boolean enabled;
        private final int min;
        private final int max;

        FakeGame(String id, boolean enabled, int min, int max) {
            this.id = id;
            this.enabled = enabled;
            this.min = min;
            this.max = max;
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
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public int getPlayersRequired() {
            return min;
        }

        @Override
        public int getMinPlayers() {
            return min;
        }

        @Override
        public int getMaxPlayers() {
            return max;
        }
    }

    private Game register(String id, boolean enabled, int min, int max) {
        Game game = new FakeGame(id, enabled, min, max);
        GameManager.get().registerGame(game);
        return game;
    }

    private static Player mockPlayer(UUID uuid) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        return player;
    }

    @Test
    void joinAndLeaveSinglePlayer() {
        Game game = register("game", true, 2, 4);
        PlayerMock player = server.addPlayer();

        assertEquals(JoinResult.SUCCESS, QueueManager.get().joinQueue(player, game));
        assertTrue(QueueManager.get().isQueued(player));
        assertEquals(1, QueueManager.get().getQueuePosition(player));
        assertSame(game, QueueManager.get().getQueuedGame(player));

        assertTrue(QueueManager.get().leaveQueue(player));
        assertFalse(QueueManager.get().isQueued(player));
        assertFalse(QueueManager.get().leaveQueue(player));
    }

    @Test
    void duplicateJoinIsRejected() {
        Game game = register("game", true, 2, 4);
        PlayerMock player = server.addPlayer();
        QueueManager.get().joinQueue(player, game);

        assertEquals(JoinResult.ALREADY_QUEUED, QueueManager.get().joinQueue(player, game));
        assertEquals(1, QueueManager.get().getQueueSize(game));
    }

    @Test
    void joiningAnotherGameMovesQueue() {
        Game first = register("first", true, 2, 4);
        Game second = register("second", true, 2, 4);
        PlayerMock player = server.addPlayer();
        QueueManager.get().joinQueue(player, first);

        assertEquals(JoinResult.SUCCESS, QueueManager.get().joinQueue(player, second));
        assertSame(second, QueueManager.get().getQueuedGame(player));
        assertEquals(0, QueueManager.get().getQueueSize(first));
    }

    @Test
    void disabledGameRejectsJoins() {
        Game game = register("game", false, 2, 4);

        assertEquals(JoinResult.GAME_DISABLED, QueueManager.get().joinQueue(server.addPlayer(), game));
    }

    @Test
    void unregisterGameRemovesQueuedPlayers() {
        Game game = register("unregister-me", true, 4, 8);
        PlayerMock first = server.addPlayer();
        PlayerMock second = server.addPlayer();
        assertEquals(JoinResult.SUCCESS, QueueManager.get().joinQueue(first, game));
        assertEquals(JoinResult.SUCCESS, QueueManager.get().joinQueue(second, game));

        GameManager.get().unregisterGame(game);

        assertFalse(QueueManager.get().isQueued(first));
        assertFalse(QueueManager.get().isQueued(second));
        assertTrue(QueueManager.get().getQueue(game).isEmpty());
    }

    @Test
    void unregisterGameLeavesOtherQueuesAlone() {
        Game doomed = register("doomed", true, 4, 8);
        Game safe = register("safe", true, 4, 8);
        PlayerMock out = server.addPlayer();
        PlayerMock stays = server.addPlayer();
        QueueManager.get().joinQueue(out, doomed);
        QueueManager.get().joinQueue(stays, safe);

        GameManager.get().unregisterGame(doomed);

        assertFalse(QueueManager.get().isQueued(out));
        assertTrue(QueueManager.get().isQueued(stays));
        assertSame(safe, QueueManager.get().getQueuedGame(stays));
    }

    @Test
    void unregisterGameFiresLeaveEvents() {
        Game game = register("unregister-events", true, 4, 8);
        PlayerMock player = server.addPlayer();
        QueueManager.get().joinQueue(player, game);
        List<QueueLeaveEvent> events = new ArrayList<>();
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onLeave(QueueLeaveEvent event) {
                events.add(event);
            }
        }, MockBukkit.createMockPlugin());

        GameManager.get().unregisterGame(game);

        assertEquals(1, events.size());
        assertSame(player, events.get(0).getPlayer());
        assertSame(game, events.get(0).getGame());
        assertEquals(QueueLeaveReason.GAME_UNREGISTERED, events.get(0).getReason());
    }

    @Test
    void unregisterGameCancelsCountdown() throws Exception {
        Game game = register("unregister-countdown", true, 1, 8);
        QueueManager.get().joinQueue(server.addPlayer(), game);

        GameManager.get().unregisterGame(game);

        Field countdowns = QueueManager.class.getDeclaredField("countdowns");
        countdowns.setAccessible(true);
        assertTrue(((Map<?, ?>) countdowns.get(QueueManager.get())).isEmpty());
        server.getScheduler().performTicks(600);
        assertTrue(MatchManager.get().getAllMatches().isEmpty());
    }

    @Test
    void cancelledEnterEventDeniesJoin() {
        Game game = register("game", true, 2, 4);
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            void onEnter(QueueEnterEvent event) {
                event.setCancelled(true);
            }
        }, org.mockbukkit.mockbukkit.MockBukkit.createMockPlugin());

        assertEquals(JoinResult.EVENT_DENIED, QueueManager.get().joinQueue(server.addPlayer(), game));
        assertEquals(0, QueueManager.get().getQueueSize(game));
    }

    @Test
    void playerInMatchCannotQueue() {
        Game game = register("game", true, 2, 4);
        PlayerMock player = server.addPlayer();
        GameManager.get().populateMatch(new Match(game, List.of(player), null));
        try {
            assertEquals(JoinResult.ALREADY_IN_MATCH, QueueManager.get().joinQueue(player, game));
        } finally {
            MatchManager.get().getAllMatches().forEach(GameManager.get()::depopulateMatch);
        }
    }

    @Test
    void groupJoinIsAllOrNothing() {
        Game game = register("game", true, 2, 4);
        PlayerMock queued = server.addPlayer();
        PlayerMock fresh = server.addPlayer();
        QueueManager.get().joinQueue(queued, game);

        assertEquals(JoinResult.ALREADY_QUEUED, QueueManager.get().joinQueue(List.of(queued, fresh), game));
        assertFalse(QueueManager.get().isQueued(fresh));
    }

    @Test
    void groupJoinAppendsContiguously() {
        Game game = register("game", true, 5, 8);
        PlayerMock first = server.addPlayer();
        PlayerMock second = server.addPlayer();

        assertEquals(JoinResult.SUCCESS, QueueManager.get().joinQueue(List.of(first, second), game));
        assertEquals(1, QueueManager.get().getQueuePosition(first));
        assertEquals(2, QueueManager.get().getQueuePosition(second));
    }

    @Test
    void emptyGroupJoinIsDenied() {
        Game game = register("game", true, 2, 4);

        assertEquals(JoinResult.EVENT_DENIED, QueueManager.get().joinQueue(List.of(), game));
        assertEquals(0, QueueManager.get().getQueueSize(game));
    }

    @Test
    void perGameCountdownOverrideWinsOverConfig() {
        Game standard = register("standard", true, 1, 4);
        Game custom = new FakeGame("custom", true, 1, 4) {
            @Override
            public java.util.OptionalDouble getQueueCountdownSeconds() {
                return java.util.OptionalDouble.of(3.0);
            }
        };

        assertEquals(20.0, QueueManager.get().getCountdownSeconds(standard));
        assertEquals(3.0, QueueManager.get().getCountdownSeconds(custom));
    }

    @Test
    void identityIsUuidBasedNotInstanceBased() {
        Game game = register("game", true, 2, 4);
        PlayerMock player = server.addPlayer();
        QueueManager.get().joinQueue(player, game);

        // A different Player object for the same online player keeps position 1.
        assertEquals(1, QueueManager.get().getQueuePosition(mockPlayer(player.getUniqueId())));
        assertTrue(QueueManager.get().isQueued(mockPlayer(player.getUniqueId())));
        assertFalse(QueueManager.get().isQueued(mockPlayer(UUID.randomUUID())));
    }

    @Test
    void returnedQueueIsDefensiveCopy() {
        Game game = register("game", true, 2, 4);
        QueueManager.get().joinQueue(server.addPlayer(), game);

        assertThrows(UnsupportedOperationException.class, () -> QueueManager.get().getQueue(game).clear());
        assertEquals(1, QueueManager.get().getQueueSize(game));
    }

    @Test
    void forceStartEmptyQueueFails() {
        Game game = register("game", true, 2, 4);

        assertFalse(QueueManager.get().forceStart(game));
    }

    @Test
    void failedStartKeepsQueueOrder() {
        // No maps registered: startMatch returns null, players go back to the front.
        Game game = register("game", true, 1, 4);
        PlayerMock player = server.addPlayer();
        QueueManager.get().joinQueue(player, game);

        assertTrue(QueueManager.get().forceStart(game));
        assertTrue(QueueManager.get().isQueued(player));
        assertEquals(1, QueueManager.get().getQueuePosition(player));
    }

    @Test
    void countdownExpiryRetriesWithoutLosingQueue() {
        // min 1 starts the countdown on join; no maps means every attempt fails
        // and requeues instead of recursing (the old StackOverflow path).
        Game game = register("game", true, 1, 4);
        PlayerMock player = server.addPlayer();
        QueueManager.get().joinQueue(player, game);

        server.getScheduler().performTicks(600);

        assertTrue(QueueManager.get().isQueued(player));
        // The failure notice proves the countdown actually expired and the
        // start was attempted (otherwise no message would have been sent).
        assertNotNull(player.nextComponentMessage());
    }

    @Test
    void leavingBelowMinimumCancelsCountdown() {
        Game game = register("game", true, 2, 4);
        PlayerMock first = server.addPlayer();
        PlayerMock second = server.addPlayer();
        QueueManager.get().joinQueue(first, game);
        QueueManager.get().joinQueue(second, game);

        QueueManager.get().leaveQueue(first);
        server.getScheduler().performTicks(600);

        // Countdown was cancelled: no match attempt moved the remaining player.
        assertTrue(QueueManager.get().isQueued(second));
        assertEquals(1, QueueManager.get().getQueueSize(game));
        // ...and no failure notice was sent, so no start was attempted.
        assertNull(second.nextComponentMessage());
    }

    @Test
    void orphanCountdownSelfCancels() {
        Game game = register("game", true, 2, 4);
        // Scheduled directly with nobody queued: the task must stand down
        // instead of starting a match.
        new org.drappula.arcadeCore.managers.queue.tasks.QueueCountdownTask(game)
                .runTaskTimer(core(), 0, 20);

        server.getScheduler().performTicks(100);

        assertTrue(MatchManager.get().getAllMatches().isEmpty());
    }
}
