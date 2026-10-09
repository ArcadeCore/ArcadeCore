package org.drappula.arcadeCore.util;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

/** {@code Event#callEvent} is Paper-only; the plugin manager call works everywhere. */
public final class Events {
    private Events() {
    }

    public static <T extends Event> T call(T event) {
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }
}
