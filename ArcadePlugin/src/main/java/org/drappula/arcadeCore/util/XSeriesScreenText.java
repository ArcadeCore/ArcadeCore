package org.drappula.arcadeCore.util;

import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.message.ScreenText;

/** Titles and action bars on every server version, via XSeries. */
public final class XSeriesScreenText implements ScreenText {
    @Override
    public void title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Titles.sendTitle(player, fadeIn, stay, fadeOut, title, subtitle == null ? "" : subtitle);
    }

    @Override
    public void actionBar(Player player, String text) {
        ActionBar.sendActionBar(player, text);
    }
}
