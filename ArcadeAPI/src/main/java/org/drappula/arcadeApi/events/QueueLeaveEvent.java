package org.drappula.arcadeApi.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.queue.QueueLeaveReason;
import org.jspecify.annotations.NonNull;

public class QueueLeaveEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
    @Override
    public @NonNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    private final Player player;
    private final Game game;
    private final QueueLeaveReason reason;

    public QueueLeaveEvent(Player player, Game game, QueueLeaveReason reason) {
        this.player = player;
        this.game = game;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public Game getGame() {
        return game;
    }

    public QueueLeaveReason getReason() {
        return reason;
    }
}
