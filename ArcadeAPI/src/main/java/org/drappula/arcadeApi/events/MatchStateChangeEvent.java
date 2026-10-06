package org.drappula.arcadeApi.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.MatchState;
import org.jspecify.annotations.NonNull;

public class MatchStateChangeEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
    @Override
    public @NonNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    private final IMatch match;
    private final MatchState oldState;
    private final MatchState newState;

    public MatchStateChangeEvent(IMatch match, MatchState oldState, MatchState newState) {
        this.match = match;
        this.oldState = oldState;
        this.newState = newState;
    }

    public IMatch getMatch() {
        return match;
    }

    public MatchState getOldState() {
        return oldState;
    }

    public MatchState getNewState() {
        return newState;
    }
}
