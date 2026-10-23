package org.drappula.arcadeCore.listeners;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.drappula.arcadeCore.ServerTest;
import org.drappula.arcadeCore.config.DataConfig;
import org.drappula.arcadeCore.config.MainConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Lobby-world protections: every flag defaults on, each is independently toggleable. */
class LobbyWorldListenerTest extends ServerTest {

    private static final List<String> FLAGS = Arrays.asList(
            "lobby.disable-explosions",
            "lobby.disable-block-edit",
            "lobby.disable-item-pickup-drop",
            "lobby.disable-item-frames",
            "lobby.disable-nature-spread",
            "lobby.disable-mob-weather",
            "lobby.disable-buckets",
            "lobby.disable-hanging-place",
            "lobby.disable-fire-ignition",
            "lobby.disable-entity-griefing",
            "lobby.disable-armor-stands",
            "lobby.disable-portals",
            "lobby.disable-ender-pearls",
            "lobby.disable-contraptions");

    private final LobbyWorldListener listener = new LobbyWorldListener();

    private World lobby;
    private World wilds;
    private PlayerMock lobbyPlayer;
    private PlayerMock wildPlayer;

    @BeforeEach
    void setupWorlds() throws Exception {
        lobby = server.addSimpleWorld("lobby");
        wilds = server.addSimpleWorld("wilds");
        DataConfig.setSpawnLocation(new Location(lobby, 0, 64, 0));
        lobbyPlayer = server.addPlayer();
        lobbyPlayer.teleport(new Location(lobby, 0, 65, 0));
        wildPlayer = server.addPlayer();
        wildPlayer.teleport(new Location(wilds, 0, 65, 0));
    }

    private static void flagOff(String key) {
        MainConfig.get().set(key, false);
    }

    private Block lobbyBlock() {
        return lobby.getBlockAt(0, 64, 0);
    }

    private Block wildBlock() {
        return wilds.getBlockAt(0, 64, 0);
    }

    @Test
    void allFlagsShipDefaultOn() {
        for (String key : FLAGS) {
            assertTrue(Boolean.TRUE.equals(MainConfig.get().getBoolean(key)),
                    "packaged default for " + key + " should be true");
        }
        assertTrue(Boolean.TRUE.equals(MainConfig.get().getBoolean("lobby.protection")),
                "packaged default for lobby.protection should be true");
    }

    @Test
    void masterOffRestoresVanillaExplosions() {
        MainConfig.get().set("lobby.protection", false);
        try {
            Block block = lobbyBlock();
            List<Block> blocks = new ArrayList<Block>(Arrays.asList(block));
            org.bukkit.event.entity.EntityExplodeEvent event =
                    new org.bukkit.event.entity.EntityExplodeEvent(lobbyPlayer, lobbyPlayer.getLocation(), blocks, 4.0f, org.bukkit.ExplosionResult.DESTROY);

            listener.onEntityExplode(event);

            assertFalse(event.isCancelled());
            assertEquals(1, event.blockList().size());
        } finally {
            MainConfig.get().set("lobby.protection", true);
        }
    }

    // ---- explosions ----

    @Test
    void explosionClearsBlockListInLobby() {
        Block block = lobbyBlock();
        List<Block> blocks = new ArrayList<Block>(Arrays.asList(block));
        org.bukkit.event.entity.EntityExplodeEvent event =
                new org.bukkit.event.entity.EntityExplodeEvent(lobbyPlayer, lobbyPlayer.getLocation(), blocks, 4.0f, org.bukkit.ExplosionResult.DESTROY);

        listener.onEntityExplode(event);

        assertTrue(event.blockList().isEmpty());
    }

    @Test
    void explosionBlockBreakKeptWhenFlagOff() {
        flagOff("lobby.disable-explosions");
        try {
            Block block = lobbyBlock();
            List<Block> blocks = new ArrayList<Block>(Arrays.asList(block));
            org.bukkit.event.entity.EntityExplodeEvent event =
                    new org.bukkit.event.entity.EntityExplodeEvent(lobbyPlayer, lobbyPlayer.getLocation(), blocks, 4.0f, org.bukkit.ExplosionResult.DESTROY);

            listener.onEntityExplode(event);

            assertFalse(event.isCancelled());
            assertEquals(1, event.blockList().size());
        } finally {
            MainConfig.get().set("lobby.disable-explosions", true);
        }
    }

    @Test
    void blockExplodeCancelledInLobby() {
        Block block = lobbyBlock();
        org.bukkit.event.block.BlockExplodeEvent event =
                new org.bukkit.event.block.BlockExplodeEvent(block, block.getState(), new ArrayList<Block>(Arrays.asList(block)), 4.0f, org.bukkit.ExplosionResult.DESTROY);

        listener.onBlockExplode(event);

        assertTrue(event.blockList().isEmpty());
    }

    @Test
    void explosionAllowedOutsideLobby() {
        Block block = wildBlock();
        List<Block> blocks = new ArrayList<Block>(Arrays.asList(block));
        org.bukkit.event.entity.EntityExplodeEvent event =
                new org.bukkit.event.entity.EntityExplodeEvent(wildPlayer, wildPlayer.getLocation(), blocks, 4.0f, org.bukkit.ExplosionResult.DESTROY);

        listener.onEntityExplode(event);

        assertEquals(1, event.blockList().size());
    }

    // ---- block edit ----

    @Test
    void blockBreakCancelledInLobby() {
        org.bukkit.event.block.BlockBreakEvent event =
                new org.bukkit.event.block.BlockBreakEvent(lobbyBlock(), lobbyPlayer);

        listener.onBlockBreak(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void blockPlaceCancelledInLobby() {
        Block block = lobbyBlock();
        org.bukkit.event.block.BlockPlaceEvent event =
                new org.bukkit.event.block.BlockPlaceEvent(block, block.getState(), block,
                        new ItemStack(Material.STONE), lobbyPlayer, true);

        listener.onBlockPlace(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void blockEditAllowedWhenFlagOff() {
        flagOff("lobby.disable-block-edit");
        try {
            org.bukkit.event.block.BlockBreakEvent event =
                    new org.bukkit.event.block.BlockBreakEvent(lobbyBlock(), lobbyPlayer);

            listener.onBlockBreak(event);

            assertFalse(event.isCancelled());
        } finally {
            MainConfig.get().set("lobby.disable-block-edit", true);
        }
    }

    @Test
    void blockBreakAllowedOutsideLobby() {
        org.bukkit.event.block.BlockBreakEvent event =
                new org.bukkit.event.block.BlockBreakEvent(wildBlock(), wildPlayer);

        listener.onBlockBreak(event);

        assertFalse(event.isCancelled());
    }

    // ---- pickup / drop ----

    @Test
    void pickupCancelledInLobby() {
        org.bukkit.event.player.PlayerPickupItemEvent event =
                new org.bukkit.event.player.PlayerPickupItemEvent(lobbyPlayer, mock(Item.class), 0);

        listener.onPickup(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void dropCancelledInLobby() {
        org.bukkit.event.player.PlayerDropItemEvent event =
                new org.bukkit.event.player.PlayerDropItemEvent(lobbyPlayer, mock(Item.class));

        listener.onDrop(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void pickupAllowedWhenFlagOff() {
        flagOff("lobby.disable-item-pickup-drop");
        try {
            org.bukkit.event.player.PlayerPickupItemEvent event =
                    new org.bukkit.event.player.PlayerPickupItemEvent(lobbyPlayer, mock(Item.class), 0);

            listener.onPickup(event);

            assertFalse(event.isCancelled());
        } finally {
            MainConfig.get().set("lobby.disable-item-pickup-drop", true);
        }
    }

    @Test
    void pickupAllowedOutsideLobby() {
        org.bukkit.event.player.PlayerPickupItemEvent event =
                new org.bukkit.event.player.PlayerPickupItemEvent(wildPlayer, mock(Item.class), 0);

        listener.onPickup(event);

        assertFalse(event.isCancelled());
    }

    // ---- item frames ----

    @Test
    void frameRotationCancelledInLobby() {
        org.bukkit.event.player.PlayerInteractEntityEvent event =
                new org.bukkit.event.player.PlayerInteractEntityEvent(lobbyPlayer, mock(ItemFrame.class));

        listener.onInteractEntity(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void otherEntityInteractAllowed() {
        org.bukkit.event.player.PlayerInteractEntityEvent event =
                new org.bukkit.event.player.PlayerInteractEntityEvent(lobbyPlayer, mock(Entity.class));

        listener.onInteractEntity(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void hangingBreakCancelledInLobby() {
        Hanging hanging = mock(Hanging.class);
        when(hanging.getWorld()).thenReturn(lobby);
        org.bukkit.event.hanging.HangingBreakEvent event =
                new org.bukkit.event.hanging.HangingBreakEvent(hanging,
                        org.bukkit.event.hanging.HangingBreakEvent.RemoveCause.ENTITY);

        listener.onHangingBreak(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void frameInteractAllowedWhenFlagOff() {
        flagOff("lobby.disable-item-frames");
        try {
            org.bukkit.event.player.PlayerInteractEntityEvent event =
                    new org.bukkit.event.player.PlayerInteractEntityEvent(lobbyPlayer, mock(ItemFrame.class));

            listener.onInteractEntity(event);

            assertFalse(event.isCancelled());
        } finally {
            MainConfig.get().set("lobby.disable-item-frames", true);
        }
    }

    // ---- nature ----

    @Test
    void leafDecayCancelledInLobby() {
        org.bukkit.event.block.LeavesDecayEvent event =
                new org.bukkit.event.block.LeavesDecayEvent(lobbyBlock());

        listener.onLeavesDecay(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void spreadBurnGrowFadeFormCancelledInLobby() {
        Block block = lobbyBlock();
        BlockState state = block.getState();

        org.bukkit.event.block.BlockSpreadEvent spread =
                new org.bukkit.event.block.BlockSpreadEvent(block, block, state);
        org.bukkit.event.block.BlockBurnEvent burn = new org.bukkit.event.block.BlockBurnEvent(block);
        org.bukkit.event.block.BlockGrowEvent grow = new org.bukkit.event.block.BlockGrowEvent(block, state);
        org.bukkit.event.block.BlockFadeEvent fade = new org.bukkit.event.block.BlockFadeEvent(block, state);
        org.bukkit.event.block.BlockFormEvent form = new org.bukkit.event.block.BlockFormEvent(block, state);

        listener.onBlockSpread(spread);
        listener.onBlockBurn(burn);
        listener.onBlockGrow(grow);
        listener.onBlockFade(fade);
        listener.onBlockForm(form);

        assertTrue(spread.isCancelled());
        assertTrue(burn.isCancelled());
        assertTrue(grow.isCancelled());
        assertTrue(fade.isCancelled());
        assertTrue(form.isCancelled());
    }

    @Test
    void treeGrowthCancelledInLobby() {
        org.bukkit.event.world.StructureGrowEvent event =
                new org.bukkit.event.world.StructureGrowEvent(new Location(lobby, 0, 64, 0),
                        org.bukkit.TreeType.TREE, false, null, new ArrayList<BlockState>());

        listener.onStructureGrow(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void natureAllowedWhenFlagOff() {
        flagOff("lobby.disable-nature-spread");
        try {
            org.bukkit.event.block.LeavesDecayEvent event =
                    new org.bukkit.event.block.LeavesDecayEvent(lobbyBlock());

            listener.onLeavesDecay(event);

            assertFalse(event.isCancelled());
        } finally {
            MainConfig.get().set("lobby.disable-nature-spread", true);
        }
    }

    @Test
    void leafDecayAllowedOutsideLobby() {
        org.bukkit.event.block.LeavesDecayEvent event =
                new org.bukkit.event.block.LeavesDecayEvent(wildBlock());

        listener.onLeavesDecay(event);

        assertFalse(event.isCancelled());
    }

    // ---- mobs / weather ----

    @Test
    void creatureSpawnCancelledInLobby() {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getWorld()).thenReturn(lobby);
        org.bukkit.event.entity.CreatureSpawnEvent event =
                new org.bukkit.event.entity.CreatureSpawnEvent(entity,
                        org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL);

        listener.onCreatureSpawn(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void stormBlockedButClearingAllowed() {
        org.bukkit.event.weather.WeatherChangeEvent start =
                new org.bukkit.event.weather.WeatherChangeEvent(lobby, true);
        org.bukkit.event.weather.WeatherChangeEvent stop =
                new org.bukkit.event.weather.WeatherChangeEvent(lobby, false);
        org.bukkit.event.weather.ThunderChangeEvent thunder =
                new org.bukkit.event.weather.ThunderChangeEvent(lobby, true);

        listener.onWeatherChange(start);
        listener.onWeatherChange(stop);
        listener.onThunderChange(thunder);

        assertTrue(start.isCancelled());
        assertFalse(stop.isCancelled());
        assertTrue(thunder.isCancelled());
    }

    @Test
    void spawnsAllowedWhenFlagOff() {
        flagOff("lobby.disable-mob-weather");
        try {
            LivingEntity entity = mock(LivingEntity.class);
            when(entity.getWorld()).thenReturn(lobby);
            org.bukkit.event.entity.CreatureSpawnEvent event =
                    new org.bukkit.event.entity.CreatureSpawnEvent(entity,
                            org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL);

            listener.onCreatureSpawn(event);

            assertFalse(event.isCancelled());
        } finally {
            MainConfig.get().set("lobby.disable-mob-weather", true);
        }
    }

    // ---- buckets ----

    @Test
    void bucketEmptyCancelledInLobby() {
        Block block = lobbyBlock();
        org.bukkit.event.player.PlayerBucketEmptyEvent event =
                new org.bukkit.event.player.PlayerBucketEmptyEvent(lobbyPlayer, block, BlockFace.UP,
                        Material.WATER_BUCKET, new ItemStack(Material.WATER_BUCKET));

        listener.onBucketEmpty(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void bucketFillCancelledInLobby() {
        Block block = lobbyBlock();
        org.bukkit.event.player.PlayerBucketFillEvent event =
                new org.bukkit.event.player.PlayerBucketFillEvent(lobbyPlayer, block, BlockFace.UP,
                        Material.BUCKET, new ItemStack(Material.BUCKET));

        listener.onBucketFill(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void bucketsAllowedWhenFlagOff() {
        flagOff("lobby.disable-buckets");
        try {
            Block block = lobbyBlock();
            org.bukkit.event.player.PlayerBucketEmptyEvent event =
                    new org.bukkit.event.player.PlayerBucketEmptyEvent(lobbyPlayer, block, BlockFace.UP,
                            Material.WATER_BUCKET, new ItemStack(Material.WATER_BUCKET));

            listener.onBucketEmpty(event);

            assertFalse(event.isCancelled());
        } finally {
            MainConfig.get().set("lobby.disable-buckets", true);
        }
    }

    // ---- hanging placement ----

    @Test
    void hangingPlaceCancelledInLobby() {
        Hanging hanging = mock(Hanging.class);
        when(hanging.getWorld()).thenReturn(lobby);
        org.bukkit.event.hanging.HangingPlaceEvent event =
                new org.bukkit.event.hanging.HangingPlaceEvent(hanging, lobbyPlayer, lobbyBlock(), BlockFace.NORTH, EquipmentSlot.HAND);

        listener.onHangingPlace(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void hangingPlaceAllowedWhenFlagOff() {
        flagOff("lobby.disable-hanging-place");
        try {
            Hanging hanging = mock(Hanging.class);
            when(hanging.getWorld()).thenReturn(lobby);
            org.bukkit.event.hanging.HangingPlaceEvent event =
                    new org.bukkit.event.hanging.HangingPlaceEvent(hanging, lobbyPlayer, lobbyBlock(), BlockFace.NORTH, EquipmentSlot.HAND);

            listener.onHangingPlace(event);

            assertFalse(event.isCancelled());
        } finally {
            MainConfig.get().set("lobby.disable-hanging-place", true);
        }
    }

    // ---- fire ignition ----

    @Test
    void ignitionCancelledInLobby() {
        org.bukkit.event.block.BlockIgniteEvent event =
                new org.bukkit.event.block.BlockIgniteEvent(lobbyBlock(),
                        BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, lobbyPlayer);

        listener.onBlockIgnite(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void ignitionAllowedWhenFlagOff() {
        flagOff("lobby.disable-fire-ignition");
        try {
            org.bukkit.event.block.BlockIgniteEvent event =
                    new org.bukkit.event.block.BlockIgniteEvent(lobbyBlock(),
                            BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, lobbyPlayer);

            listener.onBlockIgnite(event);

            assertFalse(event.isCancelled());
        } finally {
            MainConfig.get().set("lobby.disable-fire-ignition", true);
        }
    }

    @Test
    void ignitionAllowedOutsideLobby() {
        org.bukkit.event.block.BlockIgniteEvent event =
                new org.bukkit.event.block.BlockIgniteEvent(wildBlock(),
                        BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, wildPlayer);

        listener.onBlockIgnite(event);

        assertFalse(event.isCancelled());
    }

    // ---- entity griefing ----

    @Test
    void entityChangeBlockCancelledInLobby() {
        org.bukkit.event.entity.EntityChangeBlockEvent event =
                new org.bukkit.event.entity.EntityChangeBlockEvent(mock(Entity.class), lobbyBlock(), mock(BlockData.class));

        listener.onEntityChangeBlock(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void entityChangeBlockAllowedWhenFlagOff() {
        flagOff("lobby.disable-entity-griefing");
        try {
            org.bukkit.event.entity.EntityChangeBlockEvent event =
                    new org.bukkit.event.entity.EntityChangeBlockEvent(mock(Entity.class), lobbyBlock(), mock(BlockData.class));

            listener.onEntityChangeBlock(event);

            assertFalse(event.isCancelled());
        } finally {
            MainConfig.get().set("lobby.disable-entity-griefing", true);
        }
    }

    // ---- armor stands ----

    @Test
    void armorStandManipulateCancelledInLobby() {
        org.bukkit.event.player.PlayerArmorStandManipulateEvent event =
                new org.bukkit.event.player.PlayerArmorStandManipulateEvent(lobbyPlayer, mock(ArmorStand.class),
                        new ItemStack(Material.STONE), new ItemStack(Material.AIR), EquipmentSlot.HAND);

        listener.onArmorStandManipulate(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void armorStandAllowedWhenFlagOff() {
        flagOff("lobby.disable-armor-stands");
        try {
            org.bukkit.event.player.PlayerArmorStandManipulateEvent event =
                    new org.bukkit.event.player.PlayerArmorStandManipulateEvent(lobbyPlayer, mock(ArmorStand.class),
                            new ItemStack(Material.STONE), new ItemStack(Material.AIR), EquipmentSlot.HAND);

            listener.onArmorStandManipulate(event);

            assertFalse(event.isCancelled());
        } finally {
            MainConfig.get().set("lobby.disable-armor-stands", true);
        }
    }

    // ---- portals (travel only: creation has no 1.8 world accessor; see listener) ----

    @Test
    void portalTravelCancelledFromLobby() {
        org.bukkit.event.player.PlayerPortalEvent event =
                new org.bukkit.event.player.PlayerPortalEvent(lobbyPlayer, lobbyPlayer.getLocation(),
                        new Location(wilds, 0, 65, 0));

        listener.onPlayerPortal(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void entityPortalCancelledFromLobby() {
        Entity entity = mock(Entity.class);
        when(entity.getWorld()).thenReturn(lobby);
        org.bukkit.event.entity.EntityPortalEvent event =
                new org.bukkit.event.entity.EntityPortalEvent(entity, new Location(lobby, 0, 65, 0),
                        new Location(wilds, 0, 65, 0));

        listener.onEntityPortal(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void portalsAllowedWhenFlagOff() {
        flagOff("lobby.disable-portals");
        try {
            org.bukkit.event.player.PlayerPortalEvent event =
                    new org.bukkit.event.player.PlayerPortalEvent(lobbyPlayer, lobbyPlayer.getLocation(),
                            new Location(wilds, 0, 65, 0));

            listener.onPlayerPortal(event);

            assertFalse(event.isCancelled());
        } finally {
            MainConfig.get().set("lobby.disable-portals", true);
        }
    }

    @Test
    void portalTravelAllowedOutsideLobby() {
        org.bukkit.event.player.PlayerPortalEvent event =
                new org.bukkit.event.player.PlayerPortalEvent(wildPlayer, wildPlayer.getLocation(),
                        new Location(lobby, 0, 65, 0));

        listener.onPlayerPortal(event);

        assertFalse(event.isCancelled());
    }

    // ---- ender pearls ----

    @Test
    void pearlTeleportCancelledInLobby() {
        org.bukkit.event.player.PlayerTeleportEvent event =
                new org.bukkit.event.player.PlayerTeleportEvent(lobbyPlayer, lobbyPlayer.getLocation(),
                        new Location(wilds, 0, 65, 0), PlayerTeleportEvent.TeleportCause.ENDER_PEARL);

        listener.onPlayerTeleport(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void nonPearlTeleportAllowed() {
        org.bukkit.event.player.PlayerTeleportEvent event =
                new org.bukkit.event.player.PlayerTeleportEvent(lobbyPlayer, lobbyPlayer.getLocation(),
                        new Location(wilds, 0, 65, 0), PlayerTeleportEvent.TeleportCause.PLUGIN);

        listener.onPlayerTeleport(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void pearlsAllowedWhenFlagOff() {
        flagOff("lobby.disable-ender-pearls");
        try {
            org.bukkit.event.player.PlayerTeleportEvent event =
                    new org.bukkit.event.player.PlayerTeleportEvent(lobbyPlayer, lobbyPlayer.getLocation(),
                            new Location(wilds, 0, 65, 0), PlayerTeleportEvent.TeleportCause.ENDER_PEARL);

            listener.onPlayerTeleport(event);

            assertFalse(event.isCancelled());
        } finally {
            MainConfig.get().set("lobby.disable-ender-pearls", true);
        }
    }

    // ---- contraptions ----

    @Test
    void contraptionsFrozenInLobby() {
        Block block = lobbyBlock();
        org.bukkit.event.block.BlockPistonExtendEvent extend =
                new org.bukkit.event.block.BlockPistonExtendEvent(block, 1, BlockFace.NORTH);
        org.bukkit.event.block.BlockPistonRetractEvent retract =
                new org.bukkit.event.block.BlockPistonRetractEvent(block, new ArrayList<Block>(), BlockFace.NORTH);
        org.bukkit.event.block.BlockDispenseEvent dispense =
                new org.bukkit.event.block.BlockDispenseEvent(block, new ItemStack(Material.ARROW), new Vector(0, 0, 0));
        org.bukkit.event.block.BlockRedstoneEvent redstone =
                new org.bukkit.event.block.BlockRedstoneEvent(block, 0, 15);

        listener.onPistonExtend(extend);
        listener.onPistonRetract(retract);
        listener.onDispense(dispense);
        listener.onRedstone(redstone);

        assertTrue(extend.isCancelled());
        assertTrue(retract.isCancelled());
        assertTrue(dispense.isCancelled());
        assertEquals(redstone.getOldCurrent(), redstone.getNewCurrent());
    }

    @Test
    void contraptionsAllowedWhenFlagOff() {
        flagOff("lobby.disable-contraptions");
        try {
            Block block = lobbyBlock();
            org.bukkit.event.block.BlockPistonExtendEvent extend =
                    new org.bukkit.event.block.BlockPistonExtendEvent(block, 1, BlockFace.NORTH);

            listener.onPistonExtend(extend);

            assertFalse(extend.isCancelled());
        } finally {
            MainConfig.get().set("lobby.disable-contraptions", true);
        }
    }

    @Test
    void contraptionsAllowedOutsideLobby() {
        Block block = wildBlock();
        org.bukkit.event.block.BlockPistonExtendEvent extend =
                new org.bukkit.event.block.BlockPistonExtendEvent(block, 1, BlockFace.NORTH);

        listener.onPistonExtend(extend);

        assertFalse(extend.isCancelled());
    }
}
