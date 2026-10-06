package org.drappula.arcadeApi.systems.game;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.game.settings.GameEndSettings;
import org.drappula.arcadeApi.systems.game.settings.TeamSettings;
import org.drappula.arcadeApi.systems.map.IArcadeMap;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Convenience base: stores id/displayName/player counts so addons only override behavior. */
public abstract class AbstractGame implements Game {
    private final String id;
    private final String displayName;
    private final int minPlayers;
    private final int maxPlayers;

    protected AbstractGame(String id, String displayName, int playersRequired) {
        this(id, displayName, playersRequired, playersRequired);
    }

    protected AbstractGame(String id, String displayName, int minPlayers, int maxPlayers) {
        this.id = id;
        this.displayName = displayName;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public int getPlayersRequired() {
        return minPlayers;
    }

    @Override
    public int getMinPlayers() {
        return minPlayers;
    }

    @Override
    public int getMaxPlayers() {
        return maxPlayers;
    }

    @Override
    public GameEndSettings getGameEndSettings() {
        return GameEndSettings.DEFAULT;
    }

    @Override
    public TeamSettings getTeamSettings() {
        return TeamSettings.DISABLED;
    }

    @Override
    public @Nullable IArcadeMap createArena(World world, List<Player> players) {
        return null;
    }
}
