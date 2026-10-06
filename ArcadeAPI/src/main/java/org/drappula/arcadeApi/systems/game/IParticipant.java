package org.drappula.arcadeApi.systems.game;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.database.UserData;
import org.drappula.arcadeApi.systems.IProfile;
import org.jspecify.annotations.Nullable;

public interface IParticipant {
    Player getPlayer();
    IProfile getProfile();
    UserData getUserData();
    IMatch getMatch();
    /** Null for solo games. */
    @Nullable ITeam getTeam();
    boolean isEliminated();

    void eliminate();
    void eliminate(@Nullable Player killer);
}
