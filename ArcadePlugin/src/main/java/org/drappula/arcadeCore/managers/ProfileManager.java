package org.drappula.arcadeCore.managers;

import org.bukkit.entity.Player;
import org.drappula.arcadeApi.systems.IProfile;
import org.drappula.arcadeApi.systems.IProfileManager;
import org.drappula.arcadeCore.managers.impl.Profile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProfileManager implements IProfileManager {
    private static final ProfileManager INSTANCE = new ProfileManager();

    public static ProfileManager get() {
        return INSTANCE;
    }

    private static final Map<UUID, Profile> profileMap = new HashMap<>();

    public static Profile getProfile(Player player) {
        return profileMap.get(player.getUniqueId());
    }

    public static Profile removeProfile(Player player) {
        return profileMap.remove(player.getUniqueId());
    }

    public static Profile registerProfile(Profile profile) {
        return profileMap.put(profile.getPlayer().getUniqueId(), profile);
    }

    @Override
    public @Nullable IProfile find(Player player) {
        return getProfile(player);
    }

    @Override
    public @Nullable IProfile find(UUID uuid) {
        return profileMap.get(uuid);
    }

    @Override
    public Collection<IProfile> all() {
        return List.copyOf(new ArrayList<IProfile>(profileMap.values()));
    }
}
