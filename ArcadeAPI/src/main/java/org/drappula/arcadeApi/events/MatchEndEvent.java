package org.drappula.arcadeApi.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.jspecify.annotations.NonNull;

public class MatchEndEvent extends Event implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled;
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
    @Override
    public @NonNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    private final IMatch match;
    public IMatch getMatch() {
        return match;
    }

    public MatchEndEvent(IMatch match) {
        this.match = match;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
