package org.drappula.arcadeCore.listeners;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.StructureGrowEvent;

/**
 * Guards the lobby world against griefing and decay. Every handler below is
 * scoped to the lobby world only (never arena worlds) and gated behind its
 * own {@code lobby.*} flag, all of which default to on. Matches are
 * unaffected: they always run in throwaway arena worlds.
 */
public class LobbyWorldListener implements Listener {

    static boolean isLobbyWorld(World world) {
        return LobbyProtection.isLobbyWorld(world);
    }

    private static boolean enabled(String key) {
        return LobbyProtection.protectionEnabled(key);
    }

    // ---- explosions: keep entity effects, drop the terrain damage ----

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        if (!enabled("lobby.disable-explosions")) return;
        World world = e.getLocation() != null ? e.getLocation().getWorld() : entityWorld(e);
        if (!isLobbyWorld(world)) return;
        e.blockList().clear();
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        if (!enabled("lobby.disable-explosions")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        e.blockList().clear();
    }

    // ---- block edit ----

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!enabled("lobby.disable-block-edit")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!enabled("lobby.disable-block-edit")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        e.setCancelled(true);
    }

    // ---- items: no pickup, no dropping ----

    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        if (!enabled("lobby.disable-item-pickup-drop")) return;
        if (!isLobbyWorld(e.getPlayer().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (!enabled("lobby.disable-item-pickup-drop")) return;
        if (!isLobbyWorld(e.getPlayer().getWorld())) return;
        e.setCancelled(true);
    }

    // ---- item frames and hangings ----

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (!enabled("lobby.disable-item-frames")) return;
        if (!(e.getRightClicked() instanceof ItemFrame)) return;
        if (!isLobbyWorld(e.getPlayer().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onHangingBreak(HangingBreakEvent e) {
        if (!enabled("lobby.disable-item-frames")) return;
        if (!isLobbyWorld(e.getEntity().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent e) {
        if (!enabled("lobby.disable-hanging-place")) return;
        if (!isLobbyWorld(e.getEntity().getWorld())) return;
        e.setCancelled(true);
    }

    // ---- nature: decay, spread, burn, growth, formation ----

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent e) {
        if (!enabled("lobby.disable-nature-spread")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent e) {
        if (!enabled("lobby.disable-nature-spread")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent e) {
        if (!enabled("lobby.disable-nature-spread")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent e) {
        if (!enabled("lobby.disable-nature-spread")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent e) {
        if (!enabled("lobby.disable-nature-spread")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent e) {
        if (!enabled("lobby.disable-nature-spread")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent e) {
        if (!enabled("lobby.disable-nature-spread")) return;
        if (e.getLocation() == null || !isLobbyWorld(e.getLocation().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent e) {
        if (!enabled("lobby.disable-fire-ignition")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        e.setCancelled(true);
    }

    // ---- mobs and weather ----

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (!enabled("lobby.disable-mob-weather")) return;
        if (!isLobbyWorld(e.getEntity().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent e) {
        if (!enabled("lobby.disable-mob-weather")) return;
        // Storms may never start; an active storm may always clear.
        if (!e.toWeatherState()) return;
        if (!isLobbyWorld(e.getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onThunderChange(ThunderChangeEvent e) {
        if (!enabled("lobby.disable-mob-weather")) return;
        if (!e.toThunderState()) return;
        if (!isLobbyWorld(e.getWorld())) return;
        e.setCancelled(true);
    }

    // ---- buckets ----

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (!enabled("lobby.disable-buckets")) return;
        if (!isLobbyWorld(e.getPlayer().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (!enabled("lobby.disable-buckets")) return;
        if (!isLobbyWorld(e.getPlayer().getWorld())) return;
        e.setCancelled(true);
    }

    // ---- enderman and other entity terrain changes ----

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (!enabled("lobby.disable-entity-griefing")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        e.setCancelled(true);
    }

    // ---- armor stands ----

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        if (!enabled("lobby.disable-armor-stands")) return;
        if (!isLobbyWorld(e.getPlayer().getWorld())) return;
        e.setCancelled(true);
    }

    // ---- portals: block travel, not creation (see below) ----

    // No creation handler on purpose: PortalCreateEvent exposes no world on
    // 1.8 (only getBlocks, whose signature changed incompatibly on modern
    // versions), so it cannot be scoped to the lobby world. Creation is
    // already unreachable with the defaults: frames need block-place and
    // ignition needs fire-ignition, both off. Travel stays blocked here.

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent e) {
        if (!enabled("lobby.disable-portals")) return;
        if (!isLobbyWorld(e.getPlayer().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent e) {
        if (!enabled("lobby.disable-portals")) return;
        if (!isLobbyWorld(e.getEntity().getWorld())) return;
        e.setCancelled(true);
    }

    // ---- ender pearls (other teleports: commands, plugins, admins stay working) ----

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        if (!enabled("lobby.disable-ender-pearls")) return;
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        if (!isLobbyWorld(e.getPlayer().getWorld())) return;
        e.setCancelled(true);
    }

    // ---- contraptions: freeze pistons, dispensers and redstone ----

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (!enabled("lobby.disable-contraptions")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (!enabled("lobby.disable-contraptions")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent e) {
        if (!enabled("lobby.disable-contraptions")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onRedstone(BlockRedstoneEvent e) {
        if (!enabled("lobby.disable-contraptions")) return;
        if (!isLobbyWorld(e.getBlock().getWorld())) return;
        // Not cancellable: pin the current so nothing toggles.
        e.setNewCurrent(e.getOldCurrent());
    }

    private static World entityWorld(EntityEvent e) {
        Entity entity = e.getEntity();
        return entity == null ? null : entity.getWorld();
    }
}
