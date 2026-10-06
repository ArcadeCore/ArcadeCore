package org.drappula.arcadeApi.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ParticipantEliminateEvent extends Event implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled;
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
    public @NonNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    private final IParticipant participant;
    private final @Nullable Player killer;

    public IParticipant getParticipant() {
        return participant;
    }

    public @Nullable Player getKiller() {
        return killer;
    }

    public ParticipantEliminateEvent(IParticipant participant) {
        this(participant, null);
    }

    public ParticipantEliminateEvent(IParticipant participant, @Nullable Player killer) {
        this.participant = participant;
        this.killer = killer;
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
