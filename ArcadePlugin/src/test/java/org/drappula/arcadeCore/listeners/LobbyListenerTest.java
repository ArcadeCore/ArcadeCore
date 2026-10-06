package org.drappula.arcadeCore.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.drappula.arcadeApi.systems.game.MatchState;
import org.drappula.arcadeApi.systems.queue.JoinResult;
import org.drappula.arcadeCore.ServerTest;
import org.drappula.arcadeCore.managers.ProfileManager;
import org.drappula.arcadeCore.managers.game.GameManager;
import org.drappula.arcadeCore.managers.game.Match;
import org.drappula.arcadeCore.managers.game.MatchManager;
import org.drappula.arcadeCore.managers.queue.QueueManager;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LobbyListenerTest extends ServerTest {

    private static class EndingGame extends FakeGame {
        @Override
        public void onParticipantEliminate(IParticipant participant, org.bukkit.entity.Player killer) {
            IMatch match = participant.getMatch();
            if (match.getAliveCount() == 1) {
                match.endWithWinners(match.getAliveParticipants());
            } else if (match.getAliveCount() == 0) {
                match.end();
            }
        }
    }

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

        @Override
        public int getMinPlayers() {
            return 5;
        }

        @Override
        public int getMaxPlayers() {
            return 8;
        }
    }

    private final LobbyListener listener = new LobbyListener();

    @Test
    void joinRegistersProfileAndLobbies() {
        PlayerMock player = server.addPlayer();

        listener.onJoin(new PlayerJoinEvent(player, Component.text("hi")));

        assertNotNull(ProfileManager.getProfile(player));
        assertEquals(org.bukkit.GameMode.ADVENTURE, player.getGameMode());
    }

    @Test
    void quitLeavesQueueAndDropsProfile() {
        Game game = new FakeGame();
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();
        ProfileManager.registerProfile(new org.drappula.arcadeCore.managers.impl.Profile(player));
        QueueManager.get().joinQueue(player, game);
        assertTrue(QueueManager.get().isQueued(player));

        listener.onQuit(new PlayerQuitEvent(player, Component.text("bye"),
                PlayerQuitEvent.QuitReason.DISCONNECTED));

        assertFalse(QueueManager.get().isQueued(player));
        assertNull(ProfileManager.getProfile(player));
    }

    @Test
    void quitDuringStartingEliminates() {
        Game game = new FakeGame();
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();
        ProfileManager.registerProfile(new org.drappula.arcadeCore.managers.impl.Profile(player));
        Match match = new Match(game, new ArrayList<>(List.of(player)), null);
        GameManager.get().populateMatch(match);
        MatchManager.get().setState(match, MatchState.STARTING);

        listener.onQuit(new PlayerQuitEvent(player, Component.text("bye"),
                PlayerQuitEvent.QuitReason.DISCONNECTED));

        assertEquals(1, match.getEliminatedParticipants().size());
    }

    @Test
    void quitDuringEndingIsIgnored() {
        Game game = new FakeGame();
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();
        ProfileManager.registerProfile(new org.drappula.arcadeCore.managers.impl.Profile(player));
        Match match = new Match(game, new ArrayList<>(List.of(player)), null);
        GameManager.get().populateMatch(match);
        MatchManager.get().endMatch(match);

        listener.onQuit(new PlayerQuitEvent(player, Component.text("bye"),
                PlayerQuitEvent.QuitReason.DISCONNECTED));

        assertEquals(MatchState.ENDING, match.getState());
        assertTrue(match.getEliminatedParticipants().isEmpty());
    }

    @Test
    void lastQuitEndsMatchAndServerStaysHealthy() {
        EndingGame game = new EndingGame();
        GameManager.get().registerGame(game);
        PlayerMock first = server.addPlayer();
        PlayerMock second = server.addPlayer();
        ProfileManager.registerProfile(new org.drappula.arcadeCore.managers.impl.Profile(first));
        ProfileManager.registerProfile(new org.drappula.arcadeCore.managers.impl.Profile(second));
        Match match = new Match(game, new ArrayList<>(List.of(first, second)), null);
        GameManager.get().populateMatch(match);

        listener.onQuit(new PlayerQuitEvent(first, Component.text("bye"),
                PlayerQuitEvent.QuitReason.DISCONNECTED));
        assertEquals(1, match.getEliminatedParticipants().size());
        assertEquals(1, match.getAliveCount());

        listener.onQuit(new PlayerQuitEvent(second, Component.text("bye"),
                PlayerQuitEvent.QuitReason.DISCONNECTED));
        assertEquals(MatchState.ENDING, match.getState());
        server.getScheduler().performTicks(6 * 20 + 20);
        assertEquals(MatchState.ENDED, match.getState());
        assertTrue(MatchManager.get().getAllMatches().isEmpty());

        // Fresh players can still join and queue afterwards.
        PlayerMock newcomer = server.addPlayer();
        ProfileManager.registerProfile(new org.drappula.arcadeCore.managers.impl.Profile(newcomer));
        assertEquals(JoinResult.SUCCESS, QueueManager.get().joinQueue(newcomer, game));
    }

    @Test
    void joinClearsInventory() {
        PlayerMock player = server.addPlayer();
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE, 5));
        player.getInventory().setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_HELMET));

        listener.onJoin(new PlayerJoinEvent(player, Component.text("hi")));

        assertTrue(player.getInventory().isEmpty());
    }

    @Test
    void lobbyPlayerTakesNoDamage() {
        PlayerMock player = server.addPlayer();

        org.bukkit.event.entity.EntityDamageEvent event =
                new org.bukkit.event.entity.EntityDamageEvent(player, org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL, 5.0);
        listener.onDamage(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void matchParticipantTakesDamageNormally() {
        Game game = new FakeGame();
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();
        ProfileManager.registerProfile(new org.drappula.arcadeCore.managers.impl.Profile(player));
        Match match = new Match(game, new ArrayList<>(List.of(player)), null);
        GameManager.get().populateMatch(match);

        org.bukkit.event.entity.EntityDamageEvent event =
                new org.bukkit.event.entity.EntityDamageEvent(player, org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK, 5.0);
        listener.onDamage(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void lobbyAttackerCannotHitMatchPlayer() {
        Game game = new FakeGame();
        GameManager.get().registerGame(game);
        PlayerMock victim = server.addPlayer();
        ProfileManager.registerProfile(new org.drappula.arcadeCore.managers.impl.Profile(victim));
        Match match = new Match(game, new ArrayList<>(List.of(victim)), null);
        GameManager.get().populateMatch(match);
        PlayerMock attacker = server.addPlayer();

        org.bukkit.event.entity.EntityDamageByEntityEvent event =
                new org.bukkit.event.entity.EntityDamageByEntityEvent(attacker, victim,
                        org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK, 5.0);
        listener.onDamage(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void lobbyLosesNoHunger() {
        PlayerMock player = server.addPlayer();

        org.bukkit.event.entity.FoodLevelChangeEvent event =
                new org.bukkit.event.entity.FoodLevelChangeEvent(player, 10);
        listener.onHunger(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void matchPlayerHungerChangesNormally() {
        Game game = new FakeGame();
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();
        ProfileManager.registerProfile(new org.drappula.arcadeCore.managers.impl.Profile(player));
        Match match = new Match(game, new ArrayList<>(List.of(player)), null);
        GameManager.get().populateMatch(match);

        org.bukkit.event.entity.FoodLevelChangeEvent event =
                new org.bukkit.event.entity.FoodLevelChangeEvent(player, 10);
        listener.onHunger(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void lobbyLeftClicksAndPhysicalCancelled() {
        PlayerMock player = server.addPlayer();
        org.bukkit.block.Block block = player.getLocation().getBlock();

        org.bukkit.event.player.PlayerInteractEvent leftAir = new org.bukkit.event.player.PlayerInteractEvent(
                player, org.bukkit.event.block.Action.LEFT_CLICK_AIR,
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR), block,
                org.bukkit.block.BlockFace.NORTH);
        listener.onInteract(leftAir);
        assertTrue(leftAir.isCancelled());

        org.bukkit.event.player.PlayerInteractEvent physical = new org.bukkit.event.player.PlayerInteractEvent(
                player, org.bukkit.event.block.Action.PHYSICAL,
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR), block,
                org.bukkit.block.BlockFace.NORTH);
        listener.onInteract(physical);
        assertTrue(physical.isCancelled());
    }

    @Test
    void lobbyRightClicksAllowed() {
        PlayerMock player = server.addPlayer();
        org.bukkit.block.Block block = player.getLocation().getBlock();

        org.bukkit.event.player.PlayerInteractEvent rightAir = new org.bukkit.event.player.PlayerInteractEvent(
                player, org.bukkit.event.block.Action.RIGHT_CLICK_AIR,
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.COMPASS), block,
                org.bukkit.block.BlockFace.NORTH);
        listener.onInteract(rightAir);
        assertFalse(rightAir.isCancelled());

        org.bukkit.event.player.PlayerInteractEvent rightBlock = new org.bukkit.event.player.PlayerInteractEvent(
                player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.COMPASS), block,
                org.bukkit.block.BlockFace.NORTH);
        listener.onInteract(rightBlock);
        assertFalse(rightBlock.isCancelled());
    }

    @Test
    void quitEliminatesFromLiveMatch() {
        Game game = new FakeGame();
        GameManager.get().registerGame(game);
        PlayerMock player = server.addPlayer();
        ProfileManager.registerProfile(new org.drappula.arcadeCore.managers.impl.Profile(player));
        Match match = new Match(game, new ArrayList<>(List.of(player)), null);
        GameManager.get().populateMatch(match);

        listener.onQuit(new PlayerQuitEvent(player, Component.text("bye"),
                PlayerQuitEvent.QuitReason.DISCONNECTED));

        assertEquals(1, match.getEliminatedParticipants().size());
        assertNull(ProfileManager.getProfile(player));
    }
}
