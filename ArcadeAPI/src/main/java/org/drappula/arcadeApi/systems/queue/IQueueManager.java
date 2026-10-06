package org.drappula.arcadeApi.systems.queue;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.game.Game;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface IQueueManager {
    JoinResult joinQueue(Player player, Game game);

    /**
     * Parties queue together or not at all: validates the whole group first
     * (everyone must be queue-free and match-free), then appends contiguously
     * so team partitioning at match start keeps them together.
     */
    JoinResult joinQueue(Collection<Player> players, Game game);

    /** Returns true when the player was queued. */
    boolean leaveQueue(Player player);

    List<Player> getQueue(Game game);
    int getQueueSize(Game game);
    int getQueuePosition(Player player);
    boolean isQueued(Player player);
    @Nullable Game getQueuedGame(Player player);
    boolean forceStart(Game game);
}
