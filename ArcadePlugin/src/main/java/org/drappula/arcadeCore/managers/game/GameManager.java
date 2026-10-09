package org.drappula.arcadeCore.managers.game;

import org.drappula.arcadeCore.util.Immutable;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.game.IGameManager;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.drappula.arcadeCore.managers.ProfileManager;
import org.drappula.arcadeCore.managers.impl.Profile;
import org.drappula.arcadeCore.managers.map.MapManager;
import org.drappula.arcadeCore.managers.queue.QueueManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameManager implements IGameManager {
    private static GameManager instance;
    public static GameManager get() {
        if (instance == null) instance = new GameManager();
        return instance;
    }

    private Map<String, Game> games = new HashMap<>();
    private Map<String, List<IMatch>> matches = new HashMap<>();
    private Map<String, List<IParticipant>> participants = new HashMap<>();

    private static String key(String id) {
        return id.toLowerCase();
    }

    public Map<String, Game> getGames() {
        return games;
    }

    @Override
    public Collection<Game> getRegisteredGames() {
        return Immutable.copy(games.values());
    }

    @Override
    public boolean isRegistered(String id) {
        return id != null && games.containsKey(key(id));
    }

    public Map<String, List<IMatch>> getMatches() {
        return matches;
    }
    public Map<String, List<IParticipant>> getParticipants() {
        return participants;
    }

    public void registerGame(Game game) {
        if (game.getId().trim().isEmpty() || game.getId().contains(" ")) throw new IllegalArgumentException("Tried to register game with invalid ID (" + game.getId() + ")");
        games.put(key(game.getId()), game);
        game.onRegister();
        MapManager.get().backfillDefaultConfig(key(game.getId()));
    }
    public void unregisterGame(Game game) {
        this.unregisterGame(game.getId());
    }
    public Game getGame(String id) {
        if (id == null) return null;
        return games.get(key(id));
    }
    public void unregisterGame(String id) {
        Game removed = games.remove(key(id));
        if (removed == null) return;
        removed.onUnregister();
        QueueManager.get().removeGame(removed);
    }
    public void populateMatch(IMatch match) {
        String gameId = key(match.getGame().getId());
        matches.computeIfAbsent(gameId, k -> new ArrayList<>());
        participants.computeIfAbsent(gameId, k -> new ArrayList<>());
        matches.get(gameId).add(match);
        participants.get(gameId).addAll(match.getAliveParticipants());
        for (IParticipant participant : match.getAliveParticipants()) {
            Profile profile = ProfileManager.getProfile(participant.getPlayer());
            if (profile != null) profile.setMatch(match);
        }
    }
    public void depopulateMatch(IMatch match) {
        String gameId = key(match.getGame().getId());
        matches.computeIfAbsent(gameId, k -> new ArrayList<>());
        participants.computeIfAbsent(gameId, k -> new ArrayList<>());
        matches.get(gameId).remove(match);
        participants.get(gameId).removeAll(match.getParticipants());
        for (IParticipant participant : match.getParticipants()) {
            Profile profile = ProfileManager.getProfile(participant.getPlayer());
            if (profile != null && profile.getMatch() == match) profile.setMatch(null);
        }
        for (org.bukkit.entity.Player spectator : match.getSpectatingPlayers()) {
            Profile profile = ProfileManager.getProfile(spectator);
            if (profile != null && profile.getMatch() == match) profile.setMatch(null);
        }
    }
    public void reload() {
        for (Game game : Immutable.copy(games.values())) {
            try {
                game.onUnregister();
            } catch (Exception ignored) {
            }
        }
        games = new HashMap<>();
        matches = new HashMap<>();
        participants = new HashMap<>();
    }
}
