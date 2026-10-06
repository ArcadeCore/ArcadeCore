package org.drappula.arcadeCore.managers.queue.tasks;

import org.bukkit.scheduler.BukkitRunnable;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeCore.managers.queue.QueueManager;

public class QueueCountdownTask extends BukkitRunnable {
    private float timeLeft;
    private final Game game;

    public QueueCountdownTask(Game game) {
        this.game = game;
        this.timeLeft = (float) QueueManager.get().getCountdownSeconds(game);
    }

    @Override
    public void run() {
        int queueSize = QueueManager.get().getQueue(game).size();
        if (queueSize < game.getMinPlayers()) {
            QueueManager.get().cancelCountdown(game);
            this.cancel();
            return;
        }
        if (timeLeft <= 0 || queueSize >= game.getMaxPlayers()) {
            QueueManager.get().startQueuedMatch(game);
            this.cancel();
            return;
        }
        timeLeft--;
    }
}
