package org.drappula.arcadeCore.managers;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.IProfile;
import org.drappula.arcadeCore.managers.impl.Profile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileManagerTest {

    private final List<Player> tracked = new ArrayList<>();

    private Player player() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        tracked.add(player);
        return player;
    }

    @AfterEach
    void clean() {
        for (Player player : tracked) ProfileManager.removeProfile(player);
    }

    @Test
    void registerAndGetByPlayer() {
        Player player = player();
        Profile profile = new Profile(player);

        ProfileManager.registerProfile(profile);

        assertSame(profile, ProfileManager.getProfile(player));
        assertSame(profile, ProfileManager.get().find(player));
        assertSame(profile, ProfileManager.get().find(player.getUniqueId()));
    }

    @Test
    void unknownPlayerResolvesToNull() {
        Player player = player();

        assertNull(ProfileManager.getProfile(player));
        assertNull(ProfileManager.get().find(player));
        assertNull(ProfileManager.get().find(player.getUniqueId()));
    }

    @Test
    void removeDropsProfile() {
        Player player = player();
        ProfileManager.registerProfile(new Profile(player));

        ProfileManager.removeProfile(player);

        assertNull(ProfileManager.getProfile(player));
    }

    @Test
    void profileUserDataRoundTripsThroughDatabase() throws Exception {
        org.drappula.arcadeCore.database.TestDb.connect();
        try {
            Player player = player();
            Profile profile = new Profile(player);
            when(player.getName()).thenReturn("name");

            assertEquals(player.getUniqueId(), profile.getUserData().getUuid());
            assertEquals("name", profile.getUserData().getUsername());
        } finally {
            org.drappula.arcadeCore.database.TestDb.disconnect();
        }
    }

    @Test
    void allContainsRegisteredProfiles() {
        Player player = player();
        Profile profile = new Profile(player);
        ProfileManager.registerProfile(profile);

        var all = ProfileManager.get().all();

        assertTrue(all.contains((IProfile) profile));
    }
}
