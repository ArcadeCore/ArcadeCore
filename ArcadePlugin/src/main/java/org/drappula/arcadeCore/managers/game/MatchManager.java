package org.drappula.arcadeCore.managers.game;

import org.drappula.arcadeCore.util.Events;
import org.drappula.arcadeCore.util.Immutable;
import java.util.Collections;
import org.drappula.arcadeCore.util.Log;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.events.MatchEndEvent;
import org.drappula.arcadeApi.events.MatchStartEvent;
import org.drappula.arcadeApi.events.MatchStateChangeEvent;
import org.drappula.arcadeApi.systems.game.*;
import org.drappula.arcadeApi.systems.game.settings.EndFlyEnabled;
import org.drappula.arcadeApi.systems.game.settings.EndGameMode;
import org.drappula.arcadeApi.systems.game.settings.GameEndSettings;
import org.drappula.arcadeApi.systems.map.IArcadeMap;
import org.drappula.arcadeCore.ArcadeCore;
import org.drappula.arcadeCore.config.MainConfig;
import org.drappula.arcadeCore.config.MessagesConfig;
import org.drappula.arcadeCore.managers.game.tasks.MatchEndTask;
import org.drappula.arcadeCore.managers.game.tasks.MatchStartTask;
import org.drappula.arcadeCore.managers.map.MapManager;
import org.drappula.arcadeCore.managers.world.ArenaWorldManager;
import org.drappula.arcadeCore.util.MessageUtil;
import org.drappula.arcadeCore.util.PlayerUtil;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class MatchManager implements IMatchManager {
    private static MatchManager instance;
    public static MatchManager get() {
        if (instance == null) instance = new MatchManager();
        return instance;
    }

    public Map<String, List<IMatch>> getMatches() {
        return GameManager.get().getMatches();
    }

    public List<IMatch> getMatchesForGame(String gameId) {
        List<IMatch> found = GameManager.get().getMatches().get(gameId.toLowerCase());
        return found == null ? Collections.emptyList() : Immutable.copy(found);
    }

    public List<IMatch> getMatchesForGame(Game game) {
        return getMatchesForGame(game.getId());
    }

    @Override
    public List<IMatch> getAllMatches() {
        List<IMatch> all = new ArrayList<>();
        for (List<IMatch> matches : GameManager.get().getMatches().values()) {
            all.addAll(matches);
        }
        return Immutable.copy(all);
    }

    @Override
    public Optional<IMatch> getMatch(Player player) {
        UUID uuid = player.getUniqueId();
        for (List<IMatch> matches : GameManager.get().getMatches().values()) {
            for (IMatch match : matches) {
                for (IParticipant participant : match.getParticipants()) {
                    if (participant.getPlayer().getUniqueId().equals(uuid)) return Optional.of(match);
                }
                for (Player spectator : match.getSpectatingPlayers()) {
                    if (spectator.getUniqueId().equals(uuid)) return Optional.of(match);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<IMatch> getMatchById(UUID matchId) {
        for (List<IMatch> matches : GameManager.get().getMatches().values()) {
            for (IMatch match : matches) {
                if (match.getId().equals(matchId)) return Optional.of(match);
            }
        }
        return Optional.empty();
    }

    public void setState(IMatch match, MatchState newState) {
        MatchState oldState = match.getState();
        if (oldState == newState) return;
        match.setState(newState);
        Events.call(new MatchStateChangeEvent(match, oldState, newState));
    }

    public @Nullable Match startMatch(Game game, List<Player> players) {
        if (!game.isEnabled()) {
            throw new IllegalStateException("Tried to start a match for a disabled game! (" + game.getId() + ")");
        }
        World arenaWorld = ArenaWorldManager.get().create(game.getId());
        IArcadeMap map = null;
        try {
            map = game.createArena(arenaWorld, players);
        } catch (RuntimeException e) {
            ArenaWorldManager.get().destroy(arenaWorld);
            throw e;
        }
        if (map == null) {
            ArenaWorldManager.get().destroy(arenaWorld);
            Optional<IArcadeMap> acquiredMap = MapManager.get().acquireMap(game.getId());
            if (!acquiredMap.isPresent()) {
                Log.warn("No available map to start a match for game {}", game.getId());
                return null;
            }
            map = acquiredMap.get();
        }
        if (map.getSpawnPoints().size() < players.size()) {
            Log.warn("Map {} does not have enough spawn points for game {} ({} needed, {} available)",
                    map.getId(), game.getId(), players.size(), map.getSpawnPoints().size());
            MapManager.get().releaseMap(map);
            ArenaWorldManager.get().destroy(arenaWorld);
            return null;
        }
        Match match = new Match(game, players, map);
        GameManager.get().populateMatch(match);
        MatchStartEvent startEvent = new MatchStartEvent(match);
        Events.call(startEvent);
        if (startEvent.isCancelled()) {
            GameManager.get().depopulateMatch(match);
            MapManager.get().releaseMap(map);
            ArenaWorldManager.get().destroy(arenaWorld);
            return null;
        }
        setState(match, MatchState.STARTING);
        List<Location> spawnPoints = map.getSpawnPoints();
        for (int i = 0; i < players.size(); i++) {
            PlayerUtil.teleportToMatch(players.get(i), spawnPoints.get(i));
        }
        if (game.getMatchStartSettings().isCagesEnabled()) {
            SpawnCages.buildAll(match);
        }
        new MatchStartTask(match).runTaskTimer(ArcadeCore.get(), 0, 20);
        return match;
    }

    public void endMatch(IMatch match) {
        if (match.getState() == MatchState.ENDING || match.getState() == MatchState.ENDED) return;
        MatchEndEvent event = new MatchEndEvent(match);
        Events.call(event);
        if (event.isCancelled()) return;
        setState(match, MatchState.ENDING);
        SpawnCages.clear(match);
        match.broadcast(MessagesConfig.get().getString("match-ended"));
        if (!match.getWinnerParticipants().isEmpty()) {
            String winners = match.getWinnerParticipants().stream()
                    .map(winner -> winner.getPlayer().getName())
                    .collect(Collectors.joining(", "));
            match.broadcast(MessagesConfig.get().getString("match-won"),
                    "winners", winners);
        }
        new MatchEndTask(match).runTaskTimer(ArcadeCore.get(), 0, 20);
    }

    public void eliminateParticipant(IParticipant participant) {
        if (participant instanceof Participant) ((Participant) participant).setEliminated(true);
        IMatch match = participant.getMatch();
        if (match instanceof Match) {
            Match concrete = (Match) match;
            concrete.getActiveParticipants().remove(participant);
            concrete.getEliminatedParticipantsInternal().add(participant);
            if (!concrete.getSpectatingPlayersInternal().contains(participant.getPlayer())) {
                concrete.getSpectatingPlayersInternal().add(participant.getPlayer());
            }
        }
        List<IParticipant> roster = GameManager.get().getParticipants()
                .computeIfAbsent(match.getGame().getId().toLowerCase(), k -> new ArrayList<>());
        roster.remove(participant);
        GameEndSettings settings = match.getGame().getGameEndSettings();
        GameMode defaultGameMode = GameMode.valueOf(MainConfig.get().getString("match.end.gamemode"));
        boolean defaultFlyEnabled = MainConfig.get().getBoolean("match.end.fly-enabled");
        participant.getPlayer().setGameMode(settings.getGameMode() == EndGameMode.DEFAULT ? defaultGameMode : settings.getGameMode().getGameMode());
        boolean fly = settings.isFlyEnabled() == EndFlyEnabled.DEFAULT ? defaultFlyEnabled : (settings.isFlyEnabled() == EndFlyEnabled.TRUE);
        participant.getPlayer().setAllowFlight(fly);
        participant.getPlayer().setFlying(fly);

        for (IParticipant remaining : match.getAliveParticipants()) {
            MessageUtil.sendMessage(remaining.getPlayer(), MessagesConfig.get().getString("participant-eliminated"), "participant", participant.getPlayer().getName());
        }
    }
}
