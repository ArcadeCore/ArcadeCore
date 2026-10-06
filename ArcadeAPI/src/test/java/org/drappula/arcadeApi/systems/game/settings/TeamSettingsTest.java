package org.drappula.arcadeApi.systems.game.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TeamSettingsTest {

    @Test
    void disabledConstantIsNotEnabled() {
        assertFalse(TeamSettings.DISABLED.isEnabled());
    }

    @Test
    void zeroTeamSizeIsNotEnabled() {
        assertFalse(new TeamSettings(0, 4, false).isEnabled());
    }

    @Test
    void zeroMaxTeamsIsNotEnabled() {
        assertFalse(new TeamSettings(4, 0, false).isEnabled());
    }

    @Test
    void positiveSizesAreEnabled() {
        assertTrue(new TeamSettings(4, 2, false).isEnabled());
    }

    @Test
    void factoryDefaultsToNoFriendlyFire() {
        TeamSettings settings = TeamSettings.of(5, 2);

        assertEquals(5, settings.getTeamSize());
        assertEquals(2, settings.getMaxTeams());
        assertFalse(settings.isFriendlyFire());
        assertTrue(settings.isEnabled());
    }

    @Test
    void friendlyFireFlagIsStored() {
        assertTrue(new TeamSettings(5, 2, true).isFriendlyFire());
    }
}
