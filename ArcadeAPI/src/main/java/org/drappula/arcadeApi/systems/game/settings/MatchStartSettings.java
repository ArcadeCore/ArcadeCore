package org.drappula.arcadeApi.systems.game.settings;

/**
 * How the core handles players during the match start countdown: glass
 * spawn cages around each teleport point plus movement freeze, both
 * enabled by default. Addons opt out via {@code Game.getMatchStartSettings()}.
 */
public class MatchStartSettings {
    public static final MatchStartSettings DEFAULT = new MatchStartSettings(true, true);

    private final boolean cagesEnabled;
    private final boolean movementFrozen;

    public static MatchStartSettings create(boolean cagesEnabled, boolean movementFrozen) {
        return new MatchStartSettings(cagesEnabled, movementFrozen);
    }

    private MatchStartSettings(boolean cagesEnabled, boolean movementFrozen) {
        this.cagesEnabled = cagesEnabled;
        this.movementFrozen = movementFrozen;
    }

    public boolean isCagesEnabled() {
        return cagesEnabled;
    }

    public boolean isMovementFrozen() {
        return movementFrozen;
    }
}
