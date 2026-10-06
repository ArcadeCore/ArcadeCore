package org.drappula.arcadeApi.systems.game;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.map.IArcadeMap;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IMatch {
    UUID getId();
    MatchState getState();
    void setState(MatchState state);
    Instant getStartedAt();

    /**
     * Everyone who started the match, alive or eliminated. Never mutated by elimination;
     * the returned list is a defensive copy.
     */
    List<IParticipant> getParticipants();

    /** Still in the match. Defensive copy. */
    List<IParticipant> getAliveParticipants();

    List<IParticipant> getEliminatedParticipants();
    List<IParticipant> getWinnerParticipants();
    void setWinnerParticipants(List<IParticipant> participants);

    /** Defensive copy of current spectators. */
    List<Player> getSpectatingPlayers();

    List<ITeam> getTeams();

    Game getGame();
    @Nullable IArcadeMap getMap();
    void end();

    /** Sets winners and ends atomically so stats are never skipped by forgetting the setter. */
    default void endWithWinners(List<IParticipant> winners) {
        setWinnerParticipants(winners);
        end();
    }

    default boolean isRunning() {
        return getState() == MatchState.STARTING || getState() == MatchState.STARTED;
    }

    default int getAliveCount() {
        return getAliveParticipants().size();
    }

    // Spectator control for addons (viewers, late joiners as spectators, staff).
    void addSpectator(Player player);
    void removeSpectator(Player player);
    boolean isSpectating(Player player);

    void broadcast(String miniMessage, TagResolver... resolvers);
}
