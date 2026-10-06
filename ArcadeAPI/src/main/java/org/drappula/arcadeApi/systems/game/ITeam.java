package org.drappula.arcadeApi.systems.game;

import java.util.List;

/**
 * A team inside a match. Teams only exist when {@link Game#isTeamBased()} is true;
 * the core partitions match players into teams at match start so addons don't reimplement it.
 */
public interface ITeam {
    String getId();
    String getName();
    IMatch getMatch();
    List<IParticipant> getMembers();
    List<IParticipant> getAliveMembers();

    default int getSize() {
        return getMembers().size();
    }

    default int getAliveCount() {
        return getAliveMembers().size();
    }

    default boolean isEliminated() {
        return getAliveCount() == 0;
    }
}
