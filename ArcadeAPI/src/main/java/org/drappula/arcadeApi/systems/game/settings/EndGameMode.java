package org.drappula.arcadeApi.systems.game.settings;

import org.bukkit.GameMode;

import javax.annotation.Nullable;

public enum EndGameMode {
    DEFAULT(null),
    SURVIVAL(GameMode.SURVIVAL),
    ADVENTURE(GameMode.ADVENTURE),
    SPECTATOR(GameMode.SPECTATOR),
    CREATIVE(GameMode.CREATIVE);

    private final GameMode gameMode;
    EndGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public static EndGameMode parseGameMode(@Nullable GameMode gm) {
        if (gm == null) return DEFAULT;
        for (EndGameMode mode : values()) {
            if (mode.gameMode == gm) return mode;
        }
        return DEFAULT;
    }
}
