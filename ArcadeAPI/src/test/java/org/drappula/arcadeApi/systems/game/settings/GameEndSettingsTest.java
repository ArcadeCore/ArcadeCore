package org.drappula.arcadeApi.systems.game.settings;

import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEndSettingsTest {

    @Test
    void defaultFallsBackToConfig() {
        assertSame(EndGameMode.DEFAULT, GameEndSettings.DEFAULT.getGameMode());
        assertSame(EndFlyEnabled.DEFAULT, GameEndSettings.DEFAULT.isFlyEnabled());
    }

    @Test
    void nullGameModeAndFlyParseToDefault() {
        GameEndSettings settings = GameEndSettings.create(null, (Boolean) null);

        assertSame(EndGameMode.DEFAULT, settings.getGameMode());
        assertSame(EndFlyEnabled.DEFAULT, settings.isFlyEnabled());
    }

    @Test
    void everyGameModeParses() {
        assertSame(EndGameMode.SURVIVAL, GameEndSettings.create(GameMode.SURVIVAL, null).getGameMode());
        assertSame(EndGameMode.ADVENTURE, GameEndSettings.create(GameMode.ADVENTURE, null).getGameMode());
        assertSame(EndGameMode.SPECTATOR, GameEndSettings.create(GameMode.SPECTATOR, null).getGameMode());
        assertSame(EndGameMode.CREATIVE, GameEndSettings.create(GameMode.CREATIVE, null).getGameMode());
        assertNull(EndGameMode.DEFAULT.getGameMode());
    }

    @Test
    void flyFlagParsesBothWays() {
        assertSame(EndFlyEnabled.TRUE, GameEndSettings.create(null, true).isFlyEnabled());
        assertSame(EndFlyEnabled.FALSE, GameEndSettings.create(null, false).isFlyEnabled());
        assertSame(EndFlyEnabled.TRUE, EndFlyEnabled.parseFlyEnabled(Boolean.TRUE));
        assertSame(EndFlyEnabled.FALSE, EndFlyEnabled.parseFlyEnabled(Boolean.FALSE));
        assertSame(EndFlyEnabled.DEFAULT, EndFlyEnabled.parseFlyEnabled(null));
    }

    @Test
    void booleanSetterMapsToEnum() {
        GameEndSettings settings = GameEndSettings.create(EndGameMode.SURVIVAL, EndFlyEnabled.DEFAULT);

        settings.setFlyEnabled(true);
        assertSame(EndFlyEnabled.TRUE, settings.isFlyEnabled());

        settings.setFlyEnabled(false);
        assertSame(EndFlyEnabled.FALSE, settings.isFlyEnabled());

        settings.setFlyEnabled(EndFlyEnabled.DEFAULT);
        assertSame(EndFlyEnabled.DEFAULT, settings.isFlyEnabled());

        settings.setGameMode(EndGameMode.CREATIVE);
        assertSame(GameMode.CREATIVE, settings.getGameMode().getGameMode());
    }
}
