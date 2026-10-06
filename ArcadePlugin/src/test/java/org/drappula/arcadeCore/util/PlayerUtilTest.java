package org.drappula.arcadeCore.util;

import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.drappula.arcadeCore.ServerTest;
import org.drappula.arcadeCore.managers.PlayerService;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class PlayerUtilTest extends ServerTest {

    @Test
    void resetRestoresHealthAndFlight() {
        PlayerMock player = server.addPlayer();
        player.setHealth(5);

        PlayerUtil.resetPlayerState(player, GameMode.CREATIVE, true);

        assertEquals(player.getAttribute(Attribute.MAX_HEALTH).getValue(), player.getHealth());
        assertEquals(GameMode.CREATIVE, player.getGameMode());
        assertTrue(player.getAllowFlight());
        assertTrue(player.isFlying());
    }

    @Test
    void resetDisablesFlight() {
        PlayerMock player = server.addPlayer();

        PlayerUtil.resetPlayerState(player, GameMode.SURVIVAL, false);

        assertEquals(GameMode.SURVIVAL, player.getGameMode());
        assertFalse(player.getAllowFlight());
        assertFalse(player.isFlying());
    }

    @Test
    void teleportToMatchMovesAndSurvivalizes() {
        PlayerMock player = server.addPlayer();

        PlayerUtil.teleportToMatch(player, player.getLocation().add(10, 0, 0));

        assertEquals(10, player.getLocation().getX());
        assertEquals(GameMode.SURVIVAL, player.getGameMode());
    }

    @Test
    void serviceDelegatesReset() {
        PlayerMock player = server.addPlayer();

        PlayerService.get().resetPlayerState(player, GameMode.ADVENTURE, false);

        assertEquals(GameMode.ADVENTURE, player.getGameMode());
        assertFalse(player.isFlying());
    }

    @Test
    void sendToLobbyAppliesLobbySettings() {
        PlayerMock player = server.addPlayer();

        PlayerUtil.sendToLobby(player);

        // Bundled config: lobby gamemode ADVENTURE, fly enabled, no spawn set.
        assertEquals(GameMode.ADVENTURE, player.getGameMode());
        assertTrue(player.getAllowFlight());
    }

    @Test
    void sendToLobbyFallsBackToDefaultWorldWithoutSpawn() {
        PlayerMock elsewhere = server.addPlayer();
        org.bukkit.World lobby = server.addSimpleWorld("elsewhere");
        elsewhere.teleport(new org.bukkit.Location(lobby, 10, 65, 10));

        PlayerUtil.sendToLobby(elsewhere);

        org.bukkit.World fallback = org.bukkit.Bukkit.getWorlds().get(0);
        assertEquals(fallback.getName(), elsewhere.getWorld().getName());
        assertEquals(fallback.getSpawnLocation().getBlockX(), elsewhere.getLocation().getBlockX());
        assertEquals(fallback.getSpawnLocation().getBlockZ(), elsewhere.getLocation().getBlockZ());
    }

    @Test
    void sendToLobbyTeleportsToConfiguredSpawn() throws Exception {
        PlayerMock player = server.addPlayer();
        org.bukkit.Location spawn = new org.bukkit.Location(player.getWorld(), 100, 64, 200);
        org.drappula.arcadeCore.config.DataConfig.setSpawnLocation(spawn);

        PlayerService.get().sendToLobby(player);

        assertEquals(100, player.getLocation().getX());
        assertEquals(200, player.getLocation().getZ());
    }
}
