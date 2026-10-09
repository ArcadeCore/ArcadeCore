package org.drappula.arcadeCore.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.drappula.arcadeApi.message.Messages;

/** Core-side entry point for prefix-routed messages; the work lives in {@link Messages} so addons share it. */
public final class MessageUtil {
    private MessageUtil() {
    }

    /** {@code placeholders} are alternating name/value pairs substituted for {@code <name>}. */
    public static void sendMessage(Player player, String text, String... placeholders) {
        Messages.send(player, text, placeholders);
    }

    public static void sendMessage(CommandSender sender, String text, String... placeholders) {
        Messages.send(sender, text, placeholders);
    }
}
