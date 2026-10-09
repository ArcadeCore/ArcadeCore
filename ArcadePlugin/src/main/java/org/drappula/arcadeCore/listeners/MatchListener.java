package org.drappula.arcadeCore.listeners;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.drappula.arcadeApi.systems.game.MatchState;
import org.drappula.arcadeCore.managers.game.MatchManager;

import java.util.Optional;

/** Freezes alive participants in place during the match start countdown. */
public class MatchListener implements Listener {
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        Optional<IMatch> found = MatchManager.get().getMatch(event.getPlayer());
        if (!found.isPresent()) return;
        IMatch match = found.get();
        if (match.getState() != MatchState.STARTING) return;
        if (!match.getGame().getMatchStartSettings().isMovementFrozen()) return;
        for (IParticipant participant : match.getAliveParticipants()) {
            if (participant.getPlayer() != null
                    && participant.getPlayer().getUniqueId().equals(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
