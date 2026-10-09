package org.drappula.arcadeCore.api;

import java.util.Collections;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.ArcadeAPI;
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
import org.drappula.arcadeCore.database.GameStatsManager;
import org.drappula.arcadeCore.managers.PlayerService;
import org.drappula.arcadeCore.managers.ProfileManager;
import org.drappula.arcadeCore.managers.UserDataManager;
import org.drappula.arcadeCore.managers.game.GameManager;
import org.drappula.arcadeCore.managers.game.MatchManager;
import org.drappula.arcadeCore.managers.map.MapManager;
import org.drappula.arcadeCore.managers.queue.QueueManager;
import org.drappula.arcadeCore.util.PlayerUtil;
import javax.annotation.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ArcadeAPIImpl implements ArcadeAPI {
    public Optional<UserData> getUserData(UUID uuid) throws SQLException {
        return UserDataManager.get(uuid);
    }

    @Override
    public UserData getOrCreateUserData(UUID uuid, String username) {
        return UserDataManager.getOrCreate(uuid, username);
    }

    @Override
    public void saveUserData(UserData profile) throws SQLException {
        UserDataManager.save(profile);
    }

    @Override
    public IGameManager getGameManager() {
        return GameManager.get();
    }

    @Override
    public IMatchManager getMatchManager() {
        return MatchManager.get();
    }

    @Override
    public IQueueManager getQueueManager() {
        return QueueManager.get();
    }

    @Override
    public IMapManager getMapManager() {
        return MapManager.get();
    }

    @Override
    public IGameStatsManager getStatsManager() {
        return GameStatsManager.get();
    }

    @Override
    public IProfileManager getProfileManager() {
        return ProfileManager.get();
    }

    @Override
    public IPlayerService getPlayerService() {
        return PlayerService.get();
    }

    @Override
    public @Nullable IProfile getProfile(Player player) {
        return ProfileManager.getProfile(player);
    }

    @Override
    public void sendToLobby(Player player) {
        PlayerUtil.sendToLobby(player);
    }

    @Override
    public @Nullable IParticipant getParticipant(Game game, Player player) {
        UUID uuid = player.getUniqueId();
        for (IParticipant participant : GameManager.get().getParticipants().getOrDefault(game.getId().toLowerCase(), Collections.emptyList())) {
            if (participant.getPlayer().getUniqueId().equals(uuid)) return participant;
        }
        return null;
    }
}
