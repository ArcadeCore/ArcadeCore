package org.drappula.arcadeApi.systems;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public interface IPlayerService {
    void sendToLobby(Player player);
    void resetPlayerState(Player player, GameMode gameMode, boolean flying);
}
