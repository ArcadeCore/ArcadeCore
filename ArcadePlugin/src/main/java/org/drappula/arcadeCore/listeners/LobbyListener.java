package org.drappula.arcadeCore.listeners;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.drappula.arcadeApi.systems.queue.QueueLeaveReason;
import org.drappula.arcadeCore.managers.ProfileManager;
import org.drappula.arcadeCore.managers.game.MatchManager;
import org.drappula.arcadeCore.managers.impl.Profile;
import org.drappula.arcadeCore.managers.queue.QueueManager;
import org.drappula.arcadeCore.util.PlayerUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LobbyListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        ProfileManager.registerProfile(new Profile(e.getPlayer()));
        if (e.getPlayer().getInventory() != null) e.getPlayer().getInventory().clear();
        PlayerUtil.sendToLobby(e.getPlayer());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player victim && !inMatch(victim)) {
            e.setCancelled(true);
            return;
        }
        Player attacker = null;
        if (e instanceof EntityDamageByEntityEvent byEntity) {
            if (byEntity.getDamager() instanceof Player direct) attacker = direct;
            else if (byEntity.getDamager() instanceof Projectile projectile
                    && projectile.getShooter() instanceof Player shooting) attacker = shooting;
        }
        if (attacker != null && !inMatch(attacker)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (inMatch(e.getPlayer())) return;
        Action action = e.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK
                || action == Action.PHYSICAL) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player player && !inMatch(player)) {
            e.setCancelled(true);
        }
    }

    private static boolean inMatch(Player player) {
        return MatchManager.get().getMatch(player).isPresent();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        QueueManager.get().leaveQueue(e.getPlayer(), QueueLeaveReason.QUIT);
        Optional<IMatch> match = MatchManager.get().getMatch(e.getPlayer());
        if (match.isPresent()) {
            IMatch found = match.get();
            // Eliminate the player from any match they're still alive in (copy: eliminate() mutates the list)
            for (IParticipant participant : List.copyOf(found.getAliveParticipants())) {
                if (participant.getPlayer().getUniqueId().equals(uuid) && !participant.isEliminated()) {
                    participant.eliminate();
                }
            }
            // Drop them from spectator lists so match-end teardown doesn't act on an offline player
            found.removeSpectator(e.getPlayer());
        }
        ProfileManager.removeProfile(e.getPlayer());
    }
}
