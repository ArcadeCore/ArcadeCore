package org.drappula.arcadeApi.message;

import org.bukkit.entity.Player;

/**
 * Titles and action bars, which have no common Bukkit API across 1.8 to current. ArcadeCore installs an
 * implementation backed by XSeries at startup; the default only does the parts every version can do.
 */
public interface ScreenText {
    /** Times are in ticks. {@code subtitle} may be null. */
    void title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    void actionBar(Player player, String text);
}
