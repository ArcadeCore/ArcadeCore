package org.drappula.arcadeCore.managers.game.tasks;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.scheduler.BukkitRunnable;
import org.drappula.arcadeApi.systems.game.MatchState;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.drappula.arcadeCore.config.MainConfig;
import org.drappula.arcadeCore.config.MessagesConfig;
import org.drappula.arcadeCore.managers.game.MatchManager;
import org.drappula.arcadeCore.managers.game.SpawnCages;
import org.drappula.arcadeCore.util.MessageUtil;

public class MatchStartTask extends BukkitRunnable {
    private float timeLeft;
    private final IMatch match;
    public MatchStartTask(IMatch match) {
        timeLeft = (float) match.getGame().getStartCountdownSeconds().orElse(
                MainConfig.get().getOptionalDouble("match.start-countdown").orElse(10.0));
        this.match = match;
    }
    @Override
    public void run() {
        if (timeLeft <= 0) {
            SpawnCages.clear(match);
            MatchManager.get().setState(match, MatchState.STARTED);
            for (IParticipant participant : match.getAliveParticipants()) {
                MessageUtil.sendMessage(participant.getPlayer(), MessagesConfig.get().getString("match-started"));
            }
            match.getGame().onMatchStart(match);
            this.cancel();
            return;
        }
        for (IParticipant participant : match.getAliveParticipants()) {
            MessageUtil.sendMessage(participant.getPlayer(),
                    MessagesConfig.get().getString("countdown-message-" + (timeLeft == 1 ? "singular" : "plural")), Placeholder.unparsed("time", String.valueOf((int) timeLeft)));
        }
        timeLeft--;
    }
}
