package org.drappula.arcadeCore.managers.game;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.database.UserData;
import org.drappula.arcadeApi.events.ParticipantEliminateEvent;
import org.drappula.arcadeApi.systems.IProfile;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.drappula.arcadeApi.systems.game.ITeam;
import org.drappula.arcadeApi.systems.game.MatchState;
import org.drappula.arcadeCore.managers.ProfileManager;
import org.drappula.arcadeCore.managers.UserDataManager;
import org.jspecify.annotations.Nullable;

public class Participant implements IParticipant {
    private final Player player;
    private final IMatch match;
    private ITeam team;
    private boolean eliminated = false;

    public Participant(Player player, IMatch match, @Nullable ITeam team) {
        this.player = player;
        this.match = match;
        this.team = team;
    }

    @Override
    public IProfile getProfile() {
        return ProfileManager.getProfile(player);
    }

    @Override
    public UserData getUserData() {
        return UserDataManager.getOrCreate(player.getUniqueId(), player.getName());
    }

    @Override
    public IMatch getMatch() {
        return match;
    }

    @Override
    public @Nullable ITeam getTeam() {
        return team;
    }

    void setTeam(@Nullable ITeam team) {
        this.team = team;
    }

    @Override
    public boolean isEliminated() {
        return eliminated;
    }

    /** Marks this participant eliminated without re-running elimination logic (used by {@link MatchManager}). */
    void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public void eliminate() {
        eliminate(null);
    }

    @Override
    public void eliminate(@Nullable Player killer) {
        // A death/quit can land in the window where the match is already wrapping up — just ignore it
        if (eliminated || getMatch().getState() == MatchState.ENDING || getMatch().getState() == MatchState.ENDED)
            return;
        ParticipantEliminateEvent event = new ParticipantEliminateEvent(this, killer);
        event.callEvent();
        if (event.isCancelled()) return;
        eliminated = true;
        MatchManager.get().eliminateParticipant(this);
        getMatch().getGame().onParticipantEliminate(this, killer);
    }
}
