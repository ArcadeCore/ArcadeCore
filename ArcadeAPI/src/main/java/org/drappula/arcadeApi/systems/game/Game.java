package org.drappula.arcadeApi.systems.game;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.game.settings.GameEndSettings;
import org.drappula.arcadeApi.systems.game.settings.MatchStartSettings;
import org.drappula.arcadeApi.systems.game.settings.TeamSettings;
import org.drappula.arcadeApi.systems.map.MapConfigOption;
import org.drappula.arcadeApi.systems.map.IArcadeMap;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.OptionalDouble;

public interface Game {
    default boolean isEnabled() { return true; }
    String getId();
    String getDisplayName();
    int getPlayersRequired();

    /** Lower bound on players needed to start a match. Defaults to the exact {@link #getPlayersRequired()} count. */
    default int getMinPlayers() { return getPlayersRequired(); }
    /** Upper bound on players a single match can hold. Defaults to the exact {@link #getPlayersRequired()} count. */
    default int getMaxPlayers() { return getPlayersRequired(); }

    default GameEndSettings getGameEndSettings() { return GameEndSettings.DEFAULT; }

    /** Spawn cages + movement freeze during the start countdown. Both on by default. */
    default MatchStartSettings getMatchStartSettings() { return MatchStartSettings.DEFAULT; }

    /** Whether matches for this game partition players into teams. */
    default boolean isTeamBased() { return getTeamSettings().isEnabled(); }

    /** Team layout for team-based games. Solo games return {@link TeamSettings#DISABLED}. */
    default TeamSettings getTeamSettings() { return TeamSettings.DISABLED; }

    /** Per-game queue countdown override in seconds; empty falls back to {@code queue.start-countdown}. */
    default OptionalDouble getQueueCountdownSeconds() { return OptionalDouble.empty(); }

    /** Per-game match start countdown override in seconds; empty falls back to {@code match.start-countdown}. */
    default OptionalDouble getStartCountdownSeconds() { return OptionalDouble.empty(); }

    /** Per-map tuning knobs this game declares; admins set values per map via {@code /arcade map config}. Empty by default. */
    default List<MapConfigOption> getMapConfigOptions() { return List.of(); }

    /**
     * Games that build their own arena (e.g. procedurally generated) can return a ready-to-use map here
     * instead of relying on {@code MapManager}'s static map pool. Return {@code null} (the default) to use
     * the static pool as usual. The core provisions a fresh, empty arena world per match and hands it over;
     * build inside it and never touch the play worlds (or their borders) yourself.
     */
    default @Nullable IArcadeMap createArena(World world, List<Player> players) { return null; }

    // Lifecycle callbacks so simple games don't need Bukkit listeners for framework events.
    default void onRegister() {}
    default void onUnregister() {}
    default void onMatchStart(IMatch match) {}
    default void onMatchEnd(IMatch match) {}
    default void onParticipantEliminate(IParticipant participant, @Nullable Player killer) {}
}
