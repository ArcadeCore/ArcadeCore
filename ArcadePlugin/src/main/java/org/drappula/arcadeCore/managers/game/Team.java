package org.drappula.arcadeCore.managers.game;

import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.drappula.arcadeApi.systems.game.ITeam;

import java.util.ArrayList;
import java.util.List;

public class Team implements ITeam {
    private final String id;
    private final String name;
    private final IMatch match;
    private final List<IParticipant> members = new ArrayList<>();

    public Team(String id, String name, IMatch match) {
        this.id = id;
        this.name = name;
        this.match = match;
    }

    void addMember(IParticipant participant) {
        members.add(participant);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IMatch getMatch() {
        return match;
    }

    @Override
    public List<IParticipant> getMembers() {
        return List.copyOf(members);
    }

    @Override
    public List<IParticipant> getAliveMembers() {
        List<IParticipant> alive = new ArrayList<>();
        for (IParticipant member : members) {
            if (!member.isEliminated()) alive.add(member);
        }
        return List.copyOf(alive);
    }
}
