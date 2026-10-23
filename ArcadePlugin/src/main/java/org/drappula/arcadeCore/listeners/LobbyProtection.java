package org.drappula.arcadeCore.listeners;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.drappula.arcadeCore.config.DataConfig;
import org.drappula.arcadeCore.config.MainConfig;

/** Single home for lobby-protection gates. Missing keys default to on. */
public final class LobbyProtection {
    private LobbyProtection() {
    }

    /** Master switch: {@code lobby.protection}. False restores vanilla behavior everywhere. */
    public static boolean protectionEnabled() {
        return Boolean.TRUE.equals(MainConfig.get().getBoolean("lobby.protection", true));
    }

    /** A single protection applies only when the master and its own flag are both on. */
    public static boolean protectionEnabled(String key) {
        return protectionEnabled() && Boolean.TRUE.equals(MainConfig.get().getBoolean(key, true));
    }

    /** The lobby world: the configured spawn world, or the default world when unset. */
    public static boolean isLobbyWorld(World world) {
        if (world == null) return false;
        World lobby = lobbyWorld();
        return lobby != null && world.equals(lobby);
    }

    private static World lobbyWorld() {
        Optional<Location> spawn = DataConfig.getSpawnLocation();
        if (spawn.isPresent() && spawn.get().getWorld() != null) return spawn.get().getWorld();
        if (!Bukkit.getWorlds().isEmpty()) return Bukkit.getWorlds().get(0);
        return null;
    }
}
