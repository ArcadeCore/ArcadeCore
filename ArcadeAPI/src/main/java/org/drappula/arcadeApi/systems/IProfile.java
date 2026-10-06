package org.drappula.arcadeApi.systems;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.database.UserData;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.jspecify.annotations.Nullable;

public interface IProfile {
    UserData getUserData();
    Player getPlayer();
    @Nullable IMatch getMatch();
    void setMatch(@Nullable IMatch match);

    default boolean isInMatch() {
        return getMatch() != null;
    }

    default boolean isSpectating() {
        IMatch match = getMatch();
        return match != null && match.isSpectating(getPlayer());
    }
}
