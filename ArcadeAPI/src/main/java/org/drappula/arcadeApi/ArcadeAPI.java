package org.drappula.arcadeApi;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.database.IGameStatsManager;
import org.drappula.arcadeApi.database.UserData;
import org.drappula.arcadeApi.systems.IPlayerService;
import org.drappula.arcadeApi.systems.IProfile;
import org.drappula.arcadeApi.systems.IProfileManager;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.game.IGameManager;
import org.drappula.arcadeApi.systems.game.IMatchManager;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.drappula.arcadeApi.systems.map.IMapManager;
import org.drappula.arcadeApi.systems.queue.IQueueManager;
import org.jspecify.annotations.Nullable;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public interface ArcadeAPI {
    Optional<UserData> getUserData(UUID uuid) throws SQLException;
    UserData getOrCreateUserData(UUID uuid, String username) throws SQLException;
    void saveUserData(UserData profile) throws SQLException;
    IGameManager getGameManager();
    IMatchManager getMatchManager();
    IQueueManager getQueueManager();
    IMapManager getMapManager();
    IGameStatsManager getStatsManager();
    IProfileManager getProfileManager();
    IPlayerService getPlayerService();

    @Nullable IProfile getProfile(Player player);

    default boolean isInMatch(Player player) {
        IProfile profile = getProfile(player);
        return profile != null && profile.isInMatch();
    }

    default boolean isQueued(Player player) {
        return getQueueManager().isQueued(player);
    }

    void sendToLobby(Player player);

    @Nullable IParticipant getParticipant(Game game, Player player);

    /** Kept for source/binary compatibility with addons compiled against the old boolean API. */
    @Deprecated
    default @Nullable IParticipant getParticipant(Player player) {
        for (org.drappula.arcadeApi.systems.game.IMatch match : getMatchManager().getAllMatches()) {
            for (IParticipant participant : match.getParticipants()) {
                if (participant.getPlayer().getUniqueId().equals(player.getUniqueId())) return participant;
            }
        }
        return null;
    }
}
