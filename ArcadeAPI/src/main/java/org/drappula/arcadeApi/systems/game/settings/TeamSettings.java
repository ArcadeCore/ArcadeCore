package org.drappula.arcadeApi.systems.game.settings;

/**
 * Team configuration for team-based games. Only consulted when the game reports team play.
 */
public class TeamSettings {
    public static final TeamSettings DISABLED = new TeamSettings(0, 0, false);

    private final int teamSize;
    private final int maxTeams;
    private final boolean friendlyFire;

    public TeamSettings(int teamSize, int maxTeams, boolean friendlyFire) {
        this.teamSize = teamSize;
        this.maxTeams = maxTeams;
        this.friendlyFire = friendlyFire;
    }

    public static TeamSettings of(int teamSize, int maxTeams) {
        return new TeamSettings(teamSize, maxTeams, false);
    }

    public int getTeamSize() {
        return teamSize;
    }

    public int getMaxTeams() {
        return maxTeams;
    }

    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public boolean isEnabled() {
        return teamSize > 0 && maxTeams > 0;
    }
}
