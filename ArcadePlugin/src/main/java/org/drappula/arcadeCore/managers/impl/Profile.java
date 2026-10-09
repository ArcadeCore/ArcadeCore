package org.drappula.arcadeCore.managers.impl;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.database.UserData;
import org.drappula.arcadeApi.systems.IProfile;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeCore.managers.UserDataManager;
import javax.annotation.Nullable;

public class Profile implements IProfile {
    private final Player player;
    private @Nullable IMatch match;

    public Profile(Player player) {
        this.player = player;
    }

    @Override
    public UserData getUserData() {
        return UserDataManager.getOrCreate(player.getUniqueId(), player.getName());
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public @Nullable IMatch getMatch() {
        return match;
    }

    @Override
    public void setMatch(@Nullable IMatch match) {
        this.match = match;
    }
}
