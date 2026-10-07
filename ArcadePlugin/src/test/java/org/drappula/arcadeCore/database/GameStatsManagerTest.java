package org.drappula.arcadeCore.database;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.database.GameStats;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameStatsManagerTest {

    private static class FakeGame implements Game {
        @Override
        public String getId() {
            return "game";
        }

        @Override
        public String getDisplayName() {
            return "Game";
        }

        @Override
        public int getPlayersRequired() {
            return 1;
        }
    }

    private static IParticipant participant(UUID uuid, String name) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn(name);
        IParticipant participant = mock(IParticipant.class);
        when(participant.getPlayer()).thenReturn(player);
        return participant;
    }

    private static IMatch match(Game game, List<IParticipant> winners,
                               List<IParticipant> active, List<IParticipant> eliminated) {
        IMatch match = mock(IMatch.class);
        when(match.getGame()).thenReturn(game);
        when(match.getWinnerParticipants()).thenReturn(winners);
        when(match.getParticipants()).thenReturn(active);
        when(match.getEliminatedParticipants()).thenReturn(eliminated);
        return match;
    }

    private final FakeGame game = new FakeGame();

    @BeforeEach
    void openDb() throws Exception {
        TestDb.connect();
    }

    @AfterEach
    void closeDb() throws Exception {
        TestDb.disconnect();
    }

    @Test
    void noWinnersMeansNoStatsRecorded() throws Exception {
        UUID uuid = UUID.randomUUID();
        IParticipant participant = participant(uuid, "player");

        GameStatsManager.recordMatchResult(match(game, List.of(), List.of(participant), List.of()));

        assertTrue(GameStatsManager.get().getTopPlayers("game", 10).isEmpty());
    }

    @Test
    void winnersGainWinAndOthersGainLoss() throws Exception {
        UUID winnerId = UUID.randomUUID();
        UUID loserId = UUID.randomUUID();
        IParticipant winner = participant(winnerId, "winner");
        IParticipant loser = participant(loserId, "loser");

        // Active roster holds the winner; the loser was already eliminated mid-match.
        GameStatsManager.recordMatchResult(
                match(game, List.of(winner), List.of(winner), List.of(loser)));

        Optional<GameStats> winnerStats = GameStatsManager.get().getStats(winnerId, "game");
        Optional<GameStats> loserStats = GameStatsManager.get().getStats(loserId, "game");
        assertTrue(winnerStats.isPresent());
        assertTrue(loserStats.isPresent());
        assertEquals(1, winnerStats.get().getWins());
        assertEquals(0, winnerStats.get().getLosses());
        assertEquals(0, loserStats.get().getWins());
        assertEquals(1, loserStats.get().getLosses());
    }

    @Test
    void statsAccumulateAcrossMatches() throws Exception {
        UUID uuid = UUID.randomUUID();
        IParticipant participant = participant(uuid, "player");
        IParticipant champion = participant(UUID.randomUUID(), "champion");

        GameStatsManager.recordMatchResult(match(game, List.of(participant), List.of(participant), List.of()));
        // No declared winners: the outcome is unknown and nothing is recorded.
        GameStatsManager.recordMatchResult(match(game, List.of(), List.of(participant), List.of(participant)));
        GameStatsManager.recordMatchResult(match(game, List.of(champion), List.of(champion), List.of(participant)));

        Optional<GameStats> stats = GameStatsManager.get().getStats(uuid, "game");
        assertTrue(stats.isPresent());
        assertEquals(1, stats.get().getWins());
        assertEquals(1, stats.get().getLosses());
    }

    @Test
    void unknownStatsResolveToEmpty() throws Exception {
        assertTrue(GameStatsManager.get().getStats(UUID.randomUUID(), "game").isEmpty());
    }

    @Test
    void pointsAccumulateAndTopPlayersOrderByWins() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        GameStatsManager.get().addPoints(first, "first", "game", 5);
        GameStatsManager.get().addPoints(second, "second", "game", 50);
        GameStatsManager.get().addPoints(first, "first", "game", 5);

        assertEquals(10, GameStatsManager.get().getStats(first, "game").orElseThrow().getPoints());
        List<GameStats> top = GameStatsManager.get().getTopPlayers("game", 10);
        assertEquals(2, top.size());
        // Equal wins (0): higher points first.
        assertEquals(second, top.get(0).getUuid());
        assertEquals(first, top.get(1).getUuid());
    }

    @Test
    void customStatsAccumulateAndSurfaceInGameStats() throws Exception {
        UUID id = UUID.randomUUID();
        GameStatsManager.get().addStat(id, "p", "game", "kills", 1);
        GameStatsManager.get().addStat(id, "p", "game", "kills", 2);
        GameStatsManager.get().addStat(id, "p", "game", "survival_seconds", 30);

        assertEquals(3, GameStatsManager.get().getStat(id, "game", "kills"));
        assertEquals(0, GameStatsManager.get().getStat(id, "game", "deaths"));
        assertEquals(Map.of("kills", 3, "survival_seconds", 30),
                GameStatsManager.get().getStats(id, "game").orElseThrow().getOther());
        assertThrows(IllegalArgumentException.class,
                () -> GameStatsManager.get().addStat(id, "p", "game", "bad key", 1));
    }

    @Test
    void customStatsDoNotClobberPoints() throws Exception {
        UUID id = UUID.randomUUID();
        GameStatsManager.get().addPoints(id, "p", "game", 7);
        GameStatsManager.get().addStat(id, "p", "game", "kills", 1);
        assertEquals(7, GameStatsManager.get().getStats(id, "game").orElseThrow().getPoints());
    }

    @Test
    void topOverallSumsAcrossGames() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        GameStatsManager.get().addPoints(a, "a", "g1", 3);
        GameStatsManager.get().addPoints(a, "a", "g2", 4);
        GameStatsManager.get().addPoints(b, "b", "g1", 5);
        List<GameStats> top = GameStatsManager.get().getTopOverall(10);
        assertEquals(a, top.get(0).getUuid());
        assertEquals(7, top.get(0).getPoints());
    }
}
