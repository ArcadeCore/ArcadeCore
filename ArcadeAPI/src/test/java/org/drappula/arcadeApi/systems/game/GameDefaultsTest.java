package org.drappula.arcadeApi.systems.game;

import org.drappula.arcadeApi.systems.game.settings.GameEndSettings;
import org.drappula.arcadeApi.systems.game.settings.TeamSettings;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.*;

class GameDefaultsTest {

    private static Game soloGame() {
        return new Game() {
            @Override
            public String getId() {
                return "solo";
            }

            @Override
            public String getDisplayName() {
                return "Solo";
            }

            @Override
            public int getPlayersRequired() {
                return 4;
            }
        };
    }

    @Test
    void enabledByDefault() {
        assertTrue(soloGame().isEnabled());
    }

    @Test
    void minAndMaxDefaultToPlayersRequired() {
        Game game = soloGame();

        assertEquals(4, game.getMinPlayers());
        assertEquals(4, game.getMaxPlayers());
    }

    @Test
    void endSettingsDefaultToSharedDefault() {
        assertSame(GameEndSettings.DEFAULT, soloGame().getGameEndSettings());
    }

    @Test
    void usesSharedMapPoolByDefault() {
        assertNull(soloGame().createArena(mock(World.class), List.of()));
    }

    @Test
    void soloGameIsNotTeamBased() {
        Game game = soloGame();

        assertSame(TeamSettings.DISABLED, game.getTeamSettings());
        assertFalse(game.isTeamBased());
    }

    @Test
    void countdownsFallBackToConfigByDefault() {
        Game game = soloGame();

        assertTrue(game.getQueueCountdownSeconds().isEmpty());
        assertTrue(game.getStartCountdownSeconds().isEmpty());
    }

    @Test
    void mapConfigOptionsEmptyByDefault() {
        assertTrue(soloGame().getMapConfigOptions().isEmpty());
    }

    @Test
    void matchStartSettingsDefaultToSharedDefault() {
        assertSame(org.drappula.arcadeApi.systems.game.settings.MatchStartSettings.DEFAULT,
                soloGame().getMatchStartSettings());
    }

    @Test
    void lifecycleCallbacksAreCallableNoOps() {        Game game = soloGame();

        assertDoesNotThrow(() -> {
            game.onRegister();
            game.onUnregister();
            game.onMatchStart(null);
            game.onMatchEnd(null);
            game.onParticipantEliminate(null, null);
        });
    }
}
