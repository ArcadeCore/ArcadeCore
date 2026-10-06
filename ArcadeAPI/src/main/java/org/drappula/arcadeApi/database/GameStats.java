package org.drappula.arcadeApi.database;

import java.util.UUID;

/** Read view of a row in {@code game_stats}. Written by the core on match teardown, read by addons. */
public class GameStats {
    private final UUID uuid;
    private final String gameId;
    private final int points;
    private final int wins;
    private final int losses;

    public GameStats(UUID uuid, String gameId, int points, int wins, int losses) {
        this.uuid = uuid;
        this.gameId = gameId;
        this.points = points;
        this.wins = wins;
        this.losses = losses;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getGameId() {
        return gameId;
    }

    public int getPoints() {
        return points;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }
}
