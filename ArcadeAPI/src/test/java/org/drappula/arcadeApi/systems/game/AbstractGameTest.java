package org.drappula.arcadeApi.systems.game;

import org.drappula.arcadeApi.systems.game.settings.GameEndSettings;
import org.drappula.arcadeApi.systems.game.settings.TeamSettings;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.*;

class AbstractGameTest {

    private static class SoloGame extends AbstractGame {
        SoloGame() {
            super("solo", "Solo", 4);
        }
    }

    private static class RangedGame extends AbstractGame {
        RangedGame() {
            super("ranged", "Ranged", 2, 8);
        }
    }

    @Test
    void singleCountSetsMinAndMax() {
        SoloGame game = new SoloGame();

        assertEquals("solo", game.getId());
        assertEquals("Solo", game.getDisplayName());
        assertEquals(4, game.getPlayersRequired());
        assertEquals(4, game.getMinPlayers());
        assertEquals(4, game.getMaxPlayers());
    }

    @Test
    void rangeCountsAreStoredSeparately() {
        RangedGame game = new RangedGame();

        assertEquals(2, game.getMinPlayers());
        assertEquals(8, game.getMaxPlayers());
    }

    @Test
    void teamPlayDisabledByDefault() {
        assertSame(TeamSettings.DISABLED, new SoloGame().getTeamSettings());
        assertFalse(new SoloGame().isTeamBased());
    }

    @Test
    void addonDefaultsAreSane() {
        SoloGame game = new SoloGame();

        assertNull(game.createArena(mock(World.class), List.of()));
        assertSame(org.drappula.arcadeApi.systems.game.settings.GameEndSettings.DEFAULT,
                game.getGameEndSettings());
    }
}
