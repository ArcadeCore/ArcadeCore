package org.drappula.arcadeCore.managers.game;

import org.drappula.arcadeCore.util.Immutable;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.drappula.arcadeApi.systems.game.ITeam;
import org.drappula.arcadeApi.systems.game.MatchState;
import org.drappula.arcadeApi.systems.game.settings.TeamSettings;
import org.drappula.arcadeApi.systems.map.IArcadeMap;
import org.drappula.arcadeCore.util.MessageUtil;
import javax.annotation.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;

public class Match implements IMatch {
    private final UUID id = UUID.randomUUID();
    private final Instant startedAt = Instant.now();
    private MatchState state = MatchState.LOADING;
    private final Game game;
    private final IArcadeMap map;
    private final List<IParticipant> participants = new ArrayList<>();
    private final List<IParticipant> eliminatedParticipants = new ArrayList<>();
    private List<IParticipant> winnerParticipants = new ArrayList<>();
    private final List<Player> spectatingPlayers = new ArrayList<>();
    private final List<ITeam> teams = new ArrayList<>();
    private Map<Location, org.bukkit.block.BlockState> cageSnapshot;

    public Match(Game game, List<Player> players, @Nullable IArcadeMap map) {
        this.game = game;
        this.map = map;
        List<Participant> created = new ArrayList<>();
        for (Player player : players) {
            Participant participant = new Participant(player, this, null);
            created.add(participant);
            participants.add(participant);
        }
        assignTeams(created);
    }

    private void assignTeams(List<Participant> created) {
        if (!game.isTeamBased()) return;
        TeamSettings settings = game.getTeamSettings();
        if (!settings.isEnabled()) return;
        int teamSize = Math.max(1, settings.getTeamSize());
        int maxTeams = Math.max(1, settings.getMaxTeams());
        int index = 0;
        Team current = null;
        for (Participant participant : created) {
            int teamNumber = index / teamSize + 1;
            if (teamNumber > maxTeams) {
                // More players than maxTeams * teamSize: overflow into the last team
                // rather than silently creating unbounded teams.
                current = (Team) teams.get(teams.size() - 1);
            } else if (index % teamSize == 0) {
                current = new Team("team-" + teamNumber, "Team " + teamNumber, this);
                teams.add(current);
            }
            current.addMember(participant);
            participant.setTeam(current);
            index++;
        }
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public Instant getStartedAt() {
        return startedAt;
    }

    public MatchState getState() {
        return state;
    }

    public void setState(MatchState state) {
        this.state = state;
    }

    /** Spawn-cage snapshot; null when no cages are up. Internal use. */
    public Map<Location, org.bukkit.block.BlockState> getCageSnapshotInternal() {
        return cageSnapshot;
    }

    public void setCageSnapshotInternal(Map<Location, org.bukkit.block.BlockState> cageSnapshot) {
        this.cageSnapshot = cageSnapshot;
    }

    public Game getGame() {
        return game;
    }

    @Nullable
    public IArcadeMap getMap() {
        return map;
    }

    /** Everyone who started the match. Defensive copy. */
    public List<IParticipant> getParticipants() {
        List<IParticipant> all = new ArrayList<>(participants);
        all.addAll(eliminatedParticipants);
        return Immutable.copy(all);
    }

    /** Internal live view of still-active participants. */
    List<IParticipant> getActiveParticipants() {
        return participants;
    }

    @Override
    public List<IParticipant> getAliveParticipants() {
        return Immutable.copy(participants);
    }

    public List<IParticipant> getEliminatedParticipants() {
        return Immutable.copy(eliminatedParticipants);
    }

    /** Internal mutable eliminated list for MatchManager. */
    List<IParticipant> getEliminatedParticipantsInternal() {
        return eliminatedParticipants;
    }

    public List<Player> getSpectatingPlayers() {
        return new ArrayList<>(spectatingPlayers);
    }

    /** Internal mutable spectator list for MatchManager/teardown. */
    List<Player> getSpectatingPlayersInternal() {
        return spectatingPlayers;
    }

    @Override
    public List<ITeam> getTeams() {
        return Immutable.copy(teams);
    }

    public void end() {
        MatchManager.get().endMatch(this);
    }

    @Override
    public List<IParticipant> getWinnerParticipants() {
        return Immutable.copy(winnerParticipants);
    }

    @Override
    public void setWinnerParticipants(List<IParticipant> participants) {
        winnerParticipants = new ArrayList<>(participants);
    }

    @Override
    public void addSpectator(Player player) {
        if (!spectatingPlayers.contains(player)) spectatingPlayers.add(player);
    }

    @Override
    public void removeSpectator(Player player) {
        spectatingPlayers.remove(player);
    }

    @Override
    public boolean isSpectating(Player player) {
        return spectatingPlayers.contains(player);
    }

    @Override
    public void broadcast(String text, String... placeholders) {
        for (IParticipant participant : getParticipants()) {
            MessageUtil.sendMessage(participant.getPlayer(), text, placeholders);
        }
        for (Player spectator : new ArrayList<>(spectatingPlayers)) {
            MessageUtil.sendMessage(spectator, text, placeholders);
        }
    }
}
