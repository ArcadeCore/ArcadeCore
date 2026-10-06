package org.drappula.arcadeApi.systems;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.database.UserData;
import org.drappula.arcadeApi.systems.game.IMatch;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IProfileDefaultsTest {

    private static IProfile profile(@Nullable IMatch match, Player player) {
        return new IProfile() {
            @Override
            public UserData getUserData() {
                return new UserData(UUID.randomUUID(), "name");
            }

            @Override
            public Player getPlayer() {
                return player;
            }

            @Override
            public @Nullable IMatch getMatch() {
                return match;
            }

            @Override
            public void setMatch(@Nullable IMatch match) {
            }
        };
    }

    @Test
    void inMatchOnlyWhenMatchPresent() {
        Player player = mock(Player.class);

        assertFalse(profile(null, player).isInMatch());
        assertTrue(profile(mock(IMatch.class), player).isInMatch());
    }

    @Test
    void spectatingOnlyWhenMatchReportsSpectating() {
        Player player = mock(Player.class);
        IMatch match = mock(IMatch.class);
        when(match.isSpectating(player)).thenReturn(true);
        IMatch other = mock(IMatch.class);
        when(other.isSpectating(player)).thenReturn(false);

        assertFalse(profile(null, player).isSpectating());
        assertTrue(profile(match, player).isSpectating());
        assertFalse(profile(other, player).isSpectating());
    }
}
