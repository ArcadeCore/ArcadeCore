package org.drappula.arcadeCore.managers;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.IPlayerService;
import org.drappula.arcadeCore.util.PlayerUtil;

public class PlayerService implements IPlayerService {
    private static final PlayerService INSTANCE = new PlayerService();

    public static PlayerService get() {
        return INSTANCE;
    }

    @Override
    public void sendToLobby(Player player) {
        PlayerUtil.sendToLobby(player);
    }

    @Override
    public void resetPlayerState(Player player, GameMode gameMode, boolean flying) {
        PlayerUtil.resetPlayerState(player, gameMode, flying);
    }
}
