package org.drappula.arcadeApi.systems.game.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatchStartSettingsTest {

    @Test
    void defaultsEnableCagesAndFreeze() {
        assertTrue(MatchStartSettings.DEFAULT.isCagesEnabled());
        assertTrue(MatchStartSettings.DEFAULT.isMovementFrozen());
    }

    @Test
    void createRoundTrips() {
        MatchStartSettings settings = MatchStartSettings.create(false, false);

        assertFalse(settings.isCagesEnabled());
        assertFalse(settings.isMovementFrozen());
    }
}
