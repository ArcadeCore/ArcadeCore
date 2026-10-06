package org.drappula.arcadeApi.systems.game;

import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMatchManager {
    List<IMatch> getMatchesForGame(Game game);
    List<IMatch> getMatchesForGame(String gameId);
    List<IMatch> getAllMatches();

    Optional<IMatch> getMatch(Player player);
    Optional<IMatch> getMatchById(UUID matchId);

    /** Starts a match immediately with these players (bypasses the queue). Null when no map/spawns or denied. */
    @Nullable IMatch startMatch(Game game, List<Player> players);

    void endMatch(IMatch match);
    void eliminateParticipant(IParticipant participant);
}
