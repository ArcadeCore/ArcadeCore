package org.drappula.arcadeCore.managers.queue;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.events.QueueEnterEvent;
import org.drappula.arcadeApi.events.QueueLeaveEvent;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.queue.IQueueManager;
import org.drappula.arcadeApi.systems.queue.JoinResult;
import org.drappula.arcadeApi.systems.queue.QueueLeaveReason;
import org.drappula.arcadeCore.ArcadeCore;
import org.drappula.arcadeCore.config.MainConfig;
import org.drappula.arcadeCore.config.MessagesConfig;
import org.drappula.arcadeCore.managers.game.GameManager;
import org.drappula.arcadeCore.managers.game.MatchManager;
import org.drappula.arcadeCore.managers.queue.tasks.QueueCountdownTask;
import org.drappula.arcadeCore.util.MessageUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QueueManager implements IQueueManager {
    private static QueueManager instance;
    public static QueueManager get() {
        if (instance == null) instance = new QueueManager();
        return instance;
    }

    private final Map<String, List<Player>> queues = new HashMap<>();
    private final Map<String, QueueCountdownTask> countdowns = new HashMap<>();

    private static String key(String id) {
        return id.toLowerCase();
    }

    private boolean isInAnyMatch(Player player) {
        return MatchManager.get().getMatch(player).isPresent();
    }

    @Override
    public JoinResult joinQueue(Player player, Game game) {
        if (!game.isEnabled()) return JoinResult.GAME_DISABLED;
        if (isInAnyMatch(player)) return JoinResult.ALREADY_IN_MATCH;
        if (isQueued(player)) {
            Game queued = getQueuedGame(player);
            if (queued != null && queued.getId().equalsIgnoreCase(game.getId())) return JoinResult.ALREADY_QUEUED;
            leaveQueue(player);
        }
        QueueEnterEvent event = new QueueEnterEvent(player, game);
        event.callEvent();
        if (event.isCancelled()) return JoinResult.EVENT_DENIED;

        List<Player> queue = queues.computeIfAbsent(key(game.getId()), k -> new ArrayList<>());
        queue.add(player);
        maybeStartCountdown(game);
        return JoinResult.SUCCESS;
    }

    @Override
    public JoinResult joinQueue(Collection<Player> players, Game game) {
        if (players.isEmpty()) return JoinResult.EVENT_DENIED;
        if (!game.isEnabled()) return JoinResult.GAME_DISABLED;
        List<Player> group = List.copyOf(players);
        for (Player player : group) {
            if (isQueued(player)) return JoinResult.ALREADY_QUEUED;
            if (isInAnyMatch(player)) return JoinResult.ALREADY_IN_MATCH;
        }
        for (Player player : group) {
            QueueEnterEvent event = new QueueEnterEvent(player, game);
            event.callEvent();
            if (event.isCancelled()) return JoinResult.EVENT_DENIED;
        }
        List<Player> queue = queues.computeIfAbsent(key(game.getId()), k -> new ArrayList<>());
        queue.addAll(group);
        maybeStartCountdown(game);
        return JoinResult.SUCCESS;
    }

    @Override
    public boolean leaveQueue(Player player) {
        return leaveQueue(player, QueueLeaveReason.LEAVE);
    }

    public boolean leaveQueue(Player player, QueueLeaveReason reason) {        boolean removed = false;
        UUID uuid = player.getUniqueId();
        for (Map.Entry<String, List<Player>> entry : queues.entrySet()) {
            List<Player> queue = entry.getValue();
            Player found = null;
            for (Player queued : queue) {
                if (queued.getUniqueId().equals(uuid)) {
                    found = queued;
                    break;
                }
            }
            if (found != null) {
                queue.remove(found);
                removed = true;
                Game game = GameManager.get().getGame(entry.getKey());
                if (game != null) {
                    new QueueLeaveEvent(player, game, reason).callEvent();
                    if (queue.size() < game.getMinPlayers()) {
                        cancelCountdown(game);
                    }
                }
            }
        }
        return removed;
    }

    void removeForMatchStart(Game game, List<Player> players) {        List<Player> queue = queues.get(key(game.getId()));
        if (queue == null) return;
        for (Player player : players) {
            queue.removeIf(queued -> queued.getUniqueId().equals(player.getUniqueId()));
        }
        Game resolved = GameManager.get().getGame(game.getId());
        if (resolved != null) {
            for (Player player : players) {
                new QueueLeaveEvent(player, resolved, QueueLeaveReason.MATCH_START).callEvent();
            }
        }
    }

    /**
     * Drops this game's entire queue (e.g. the game was unregistered),
     * cancelling its countdown and notifying each player via {@code QueueLeaveEvent}.
     */
    public void removeGame(Game game) {
        List<Player> queue = queues.remove(key(game.getId()));
        cancelCountdown(game);
        if (queue == null) return;
        for (Player player : queue) {
            new QueueLeaveEvent(player, game, QueueLeaveReason.GAME_UNREGISTERED).callEvent();
        }
    }

    @Override
    public List<Player> getQueue(Game game) {
        return List.copyOf(queues.getOrDefault(key(game.getId()), List.of()));
    }

    @Override
    public int getQueueSize(Game game) {
        return queues.getOrDefault(key(game.getId()), List.of()).size();
    }

    @Override
    public int getQueuePosition(Player player) {
        UUID uuid = player.getUniqueId();
        for (List<Player> queue : queues.values()) {
            for (int i = 0; i < queue.size(); i++) {
                if (queue.get(i).getUniqueId().equals(uuid)) return i + 1;
            }
        }
        return -1;
    }

    @Override
    public boolean isQueued(Player player) {
        return getQueuePosition(player) != -1;
    }

    @Override
    @Nullable
    public Game getQueuedGame(Player player) {
        UUID uuid = player.getUniqueId();
        for (Map.Entry<String, List<Player>> entry : queues.entrySet()) {
            for (Player queued : entry.getValue()) {
                if (queued.getUniqueId().equals(uuid)) {
                    return GameManager.get().getGame(entry.getKey());
                }
            }
        }
        return null;
    }

    @Override
    public boolean forceStart(Game game) {
        List<Player> queue = queues.get(key(game.getId()));
        if (queue == null || queue.isEmpty()) return false;
        startQueuedMatch(game);
        return true;
    }

    /** Cancels this game's pending queue countdown, if any (e.g. the queue dropped below the minimum). */
    public void cancelCountdown(Game game) {
        QueueCountdownTask task = countdowns.remove(key(game.getId()));
        if (task != null) task.cancel();
    }

    public double getCountdownSeconds(Game game) {
        return game.getQueueCountdownSeconds().orElse(
                MainConfig.get().getOptionalDouble("queue.start-countdown").orElse(20.0));
    }

    /** Slices up to {@code getMaxPlayers()} players off this game's queue and starts a match with them. */
    public void startQueuedMatch(Game game) {
        cancelCountdown(game);
        List<Player> queue = queues.get(key(game.getId()));
        if (queue == null || queue.isEmpty()) return;
        int count = Math.min(queue.size(), game.getMaxPlayers());
        List<Player> players = new ArrayList<>(queue.subList(0, count));
        removeForMatchStart(game, players);
        if (MatchManager.get().startMatch(game, players) == null) {
            // Start failed (no map, too few spawns, or start event denied): put the
            // players back at the front so nobody silently loses their spot.
            queue.addAll(0, players);
            // Restart the countdown without an immediate retry: maybeStartCountdown
            // would call startQueuedMatch again here (queue may still be >= max),
            // recursing until StackOverflow. The countdown task retries on its next tick.
            startCountdownIfNeeded(game);
            for (Player queuedPlayer : players) {
                MessageUtil.sendMessage(queuedPlayer, MessagesConfig.get().getString("map-unavailable"));
            }
        }
    }

    /** Starts a match immediately if the queue is full, or (re)starts the countdown at the minimum. */
    private void maybeStartCountdown(Game game) {
        List<Player> queue = queues.getOrDefault(key(game.getId()), List.of());
        if (queue.size() >= game.getMaxPlayers()) {
            startQueuedMatch(game);
        } else {
            startCountdownIfNeeded(game);
        }
    }

    /** (Re)starts the countdown if the queue is at the minimum and none is running. Never starts a match directly. */
    private void startCountdownIfNeeded(Game game) {
        List<Player> queue = queues.getOrDefault(key(game.getId()), List.of());
        if (queue.size() >= game.getMinPlayers() && !countdowns.containsKey(key(game.getId()))) {
            QueueCountdownTask task = new QueueCountdownTask(game);
            task.runTaskTimer(ArcadeCore.get(), 0, 20);
            countdowns.put(key(game.getId()), task);
        }
    }
}
