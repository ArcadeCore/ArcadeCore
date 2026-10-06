package org.drappula.arcadeCore.util;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.kyori.adventure.util.Ticks;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class MessageUtil {
    public static void sendMessage(Player player, String text) {
        sendMessage(player, text, TagResolver.empty(), 1);
    }
    public static void sendMessage(Player player, String text, TagResolver resolver) {
        sendMessage(player, text, resolver, 1);
    }
    public static void sendMessage(Player player, String text, TagResolver resolver, int bossbarProgress) {
        if (text == null || text.isEmpty()) return;
        List<String> messages = Arrays.stream(text.split("(?<!\\\\);")).toList();
        for (String message : messages) {
            if (message == null || message.isEmpty()) continue;
            String msgUnprefix = message.replaceFirst("^[a-z]+:", "");
            if (message.startsWith("title:")) player.sendTitlePart(TitlePart.TITLE, MiniMessage.miniMessage().deserialize(msgUnprefix, resolver));
            else if (message.startsWith("subtitle:")) player.sendTitlePart(TitlePart.SUBTITLE, MiniMessage.miniMessage().deserialize(msgUnprefix, resolver));
            else if (message.startsWith("titletime:")) {
                List<String> times = Arrays.stream(msgUnprefix.split(":", -1)).toList();
                if (times.size() < 3) continue;
                try {
                    Duration fadeIn = times.get(0).isEmpty() ? Ticks.duration(10) : Duration.ofMillis(Integer.parseInt(times.get(0)));
                    Duration stay = times.get(1).isEmpty() ? Ticks.duration(70) : Duration.ofMillis(Integer.parseInt(times.get(1)));
                    Duration fadeOut = times.get(2).isEmpty() ? Ticks.duration(20) : Duration.ofMillis(Integer.parseInt(times.get(2)));
                    Title.Times titleTimes = Title.Times.times(fadeIn, stay, fadeOut);
                    player.sendTitlePart(TitlePart.TIMES, titleTimes);
                } catch (NumberFormatException ignored) {
                }
            }
            else if (message.startsWith("actionbar:")) player.sendActionBar(MiniMessage.miniMessage().deserialize(msgUnprefix, resolver));
            else if (message.startsWith("message:")) {
                player.sendRichMessage(msgUnprefix.replace("\n", "<newline>"), resolver);
            }
            else if (message.startsWith("bossbar:")) {
                List<String> sections = Arrays.stream(msgUnprefix.split(":", -1)).toList();
                if (sections.size() < 4) continue;
                try {
                    String givenProgressString = sections.get(1);
                    float progress = givenProgressString.isEmpty() ? bossbarProgress : Float.parseFloat(givenProgressString);
                    progress = Math.clamp(progress, 0f, 1f);
                    BossBar.Color color;
                    try {
                        color = sections.get(2).isEmpty() ? BossBar.Color.WHITE : BossBar.Color.valueOf(sections.get(2).toUpperCase());
                    } catch (IllegalArgumentException e) {
                        color = BossBar.Color.WHITE;
                    }
                    BossBar.Overlay overlay;
                    try {
                        overlay = sections.get(3).isEmpty() ? BossBar.Overlay.PROGRESS : BossBar.Overlay.valueOf(sections.get(3).toUpperCase());
                    } catch (IllegalArgumentException e) {
                        overlay = BossBar.Overlay.PROGRESS;
                    }
                    player.showBossBar(BossBar.bossBar(MiniMessage.miniMessage().deserialize(sections.getFirst(), resolver), progress, color, overlay));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    /**
     * Command-sender variant: players get the full routing (titles, bossbar, ...),
     * non-player senders (console) receive the chat/actionbar text while
     * visual-only segments (title, subtitle, bossbar) are skipped.
     */
    public static void sendMessage(CommandSender sender, String text, TagResolver resolver) {
        if (sender instanceof Player player) {
            sendMessage(player, text, resolver);
            return;
        }
        if (text == null || text.isEmpty()) return;
        List<String> messages = Arrays.stream(text.split("(?<!\\\\);")).toList();
        for (String message : messages) {
            if (message == null || message.isEmpty()) continue;
            String msgUnprefix = message.replaceFirst("^[a-z]+:", "");
            if (message.startsWith("message:") || message.startsWith("actionbar:")) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(msgUnprefix.replace("\n", "<newline>"), resolver));
            } else if (!message.startsWith("title:") && !message.startsWith("subtitle:")
                    && !message.startsWith("titletime:") && !message.startsWith("bossbar:")) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(message, resolver));
            }
        }
    }
}
