package org.drappula.arcadeApi.systems;

import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

public interface IProfileManager {
    @Nullable IProfile find(Player player);
    @Nullable IProfile find(UUID uuid);
    Collection<IProfile> all();
}
