package org.drappula.arcadeApi;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.database.IGameStatsManager;
import org.drappula.arcadeApi.database.UserData;
import org.drappula.arcadeApi.systems.IPlayerService;
import org.drappula.arcadeApi.systems.IProfile;
import org.drappula.arcadeApi.systems.IProfileManager;
import org.drappula.arcadeApi.systems.game.Game;
import org.drappula.arcadeApi.systems.game.IGameManager;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.drappula.arcadeApi.systems.game.IMatchManager;
import org.drappula.arcadeApi.systems.game.IParticipant;
import org.drappula.arcadeApi.systems.map.IMapManager;
import org.drappula.arcadeApi.systems.queue.IQueueManager;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArcadeAPIDefaultsTest {

    private static class StubApi implements ArcadeAPI {
        private final IProfile profile;
        private final IQueueManager queues;

        StubApi(IProfile profile, IQueueManager queues) {
            this.profile = profile;
            this.queues = queues;
        }

        @Override
        public Optional<UserData> getUserData(UUID uuid) {
            return Optional.empty();
        }

        @Override
        public UserData getOrCreateUserData(UUID uuid, String username) {
            return new UserData(uuid, username);
        }

        @Override
        public void saveUserData(UserData profile) {
        }

        @Override
        public IGameManager getGameManager() {
            return null;
        }

        @Override
        public IMatchManager getMatchManager() {
            return null;
        }

        @Override
        public IQueueManager getQueueManager() {
            return queues;
        }

        @Override
        public IMapManager getMapManager() {
            return null;
        }

        @Override
        public IGameStatsManager getStatsManager() {
            return null;
        }

        @Override
        public IProfileManager getProfileManager() {
            return null;
        }

        @Override
        public IPlayerService getPlayerService() {
            return null;
        }

        @Override
        public @Nullable IProfile getProfile(Player player) {
            return profile;
        }

        @Override
        public void sendToLobby(Player player) {
        }

        @Override
        public @Nullable IParticipant getParticipant(Game game, Player player) {
            return null;
        }
    }

    @Test
    void inMatchFollowsProfile() throws SQLException {
        Player player = mock(Player.class);
        IProfile inMatch = mock(IProfile.class);
        when(inMatch.isInMatch()).thenReturn(true);

        assertTrue(new StubApi(inMatch, mock(IQueueManager.class)).isInMatch(player));
        assertFalse(new StubApi(null, mock(IQueueManager.class)).isInMatch(player));
    }

    @Test
    void queuedDelegatesToQueueManager() {
        Player player = mock(Player.class);
        IQueueManager queues = mock(IQueueManager.class);
        when(queues.isQueued(player)).thenReturn(true);

        assertTrue(new StubApi(null, queues).isQueued(player));
        assertFalse(new StubApi(null, mock(IQueueManager.class)).isQueued(player));
    }

    @Test
    void deprecatedGlobalLookupScansMatches() {
        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        IParticipant participant = mock(IParticipant.class);
        when(participant.getPlayer()).thenReturn(player);
        IMatch match = mock(IMatch.class);
        when(match.getParticipants()).thenReturn(List.of(participant));
        IMatchManager matches = mock(IMatchManager.class);
        when(matches.getAllMatches()).thenReturn(List.of(match));

        ArcadeAPI api = new StubApi(null, mock(IQueueManager.class)) {
            @Override
            public IMatchManager getMatchManager() {
                return matches;
            }
        };

        assertSame(participant, api.getParticipant(player));
        assertNull(api.getParticipant(mock(Player.class)));
    }
}
