package org.drappula.arcadeCore.managers.game.tasks;

import org.drappula.arcadeCore.util.Immutable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.drappula.arcadeApi.systems.game.MatchState;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.drappula.arcadeCore.database.GameStatsManager;
import org.drappula.arcadeCore.managers.game.GameManager;
import org.drappula.arcadeCore.managers.game.MatchManager;
import org.drappula.arcadeCore.managers.map.MapManager;
import org.drappula.arcadeCore.managers.world.ArenaWorldManager;
import org.drappula.arcadeCore.util.PlayerUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MatchEndTask extends BukkitRunnable {
    private int time = 5;
    private final IMatch match;
    public MatchEndTask(IMatch match) {
        this.match = match;
    }
    @Override
    public void run() {
        if (time == 0) {
            MatchManager.get().setState(match, MatchState.ENDED);
            match.getGame().onMatchEnd(match);
            GameStatsManager.recordMatchResult(match);
            GameManager.get().depopulateMatch(match);
            MapManager.get().releaseMap(match.getMap());
            // Spectators hold everyone eliminated during the match; leftover active
            // participants (normally none) must also make it back to the lobby.
            Set<Player> returning = new LinkedHashSet<>(match.getSpectatingPlayers());
            for (IParticipant participant : match.getParticipants()) {
                returning.add(participant.getPlayer());
            }
            for (Player player : returning) {
                // Match items stay in the match: wipe everything including armor.
                if (player.getInventory() != null) player.getInventory().clear();
                PlayerUtil.sendToLobby(player);
            }
            ArenaWorldManager.get().destroy(match.getMap() != null ? match.getMap().getWorld() : null);
            this.cancel();
            return;
        }
        if (time == 5) {
            // Copy the list: eliminateParticipant removes from the active list while we iterate
            for (IParticipant participant : Immutable.copy(match.getAliveParticipants())) {
                MatchManager.get().eliminateParticipant(participant);
            }
        }
        time--;
    }
}
