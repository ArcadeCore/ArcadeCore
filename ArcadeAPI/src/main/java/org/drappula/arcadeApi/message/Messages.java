package org.drappula.arcadeApi.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Sends the {@code prefix:text;prefix:text} message strings ArcadeCore and its addons use. Works on every
 * supported server version: colours come from {@link LegacyText}, titles and action bars go through
 * {@link ScreenText}, and boss bars degrade to an action bar where the server has no boss bar API.
 *
 * <p>Prefixes: {@code message:}, {@code title:}, {@code subtitle:}, {@code titletime:ms:ms:ms},
 * {@code actionbar:}, {@code bossbar:text:progress:color:overlay}. Escape a literal semicolon as {@code \;}.
 * Unprefixed text is dropped for players and sent as chat to the console.
 */
public final class Messages {
    private static final int DEFAULT_FADE_IN = 10;
    private static final int DEFAULT_STAY = 70;
    private static final int DEFAULT_FADE_OUT = 20;

    private static ScreenText screen;

    private Messages() {
    }

    /** Installs the title/action bar backend. Pass null to restore the default. */
    public static void useScreenText(ScreenText impl) {
        screen = impl;
    }

    private static ScreenText screen() {
        if (screen == null) screen = new ChatOnlyScreenText();
        return screen;
    }

    private static Map<String, String> pairs(String... kv) {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; kv != null && i + 1 < kv.length; i += 2) map.put(kv[i], kv[i + 1]);
        return map;
    }

    private static List<String> segments(String text) {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length() && text.charAt(i + 1) == ';') {
                cur.append(';');
                i++;
            } else if (c == ';') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    private static String strip(String segment) {
        return segment.replaceFirst("^[a-z]+:", "");
    }

    private static int ticks(String ms, int fallback) {
        if (ms.isEmpty()) return fallback;
        return Integer.parseInt(ms) / 50;
    }

    public static void send(Player player, String text, String... placeholders) {
        if (text == null || text.isEmpty()) return;
        Map<String, String> ph = pairs(placeholders);
        String title = null;
        String subtitle = null;
        int fadeIn = DEFAULT_FADE_IN;
        int stay = DEFAULT_STAY;
        int fadeOut = DEFAULT_FADE_OUT;
        boolean titleSeen = false;
        for (String segment : segments(text)) {
            if (segment.isEmpty()) continue;
            String body = LegacyText.translate(strip(segment), ph);
            if (segment.startsWith("title:")) {
                title = body;
                titleSeen = true;
            } else if (segment.startsWith("subtitle:")) {
                subtitle = body;
                titleSeen = true;
            } else if (segment.startsWith("titletime:")) {
                String[] t = strip(segment).split(":", -1);
                if (t.length < 3) continue;
                try {
                    fadeIn = ticks(t[0], DEFAULT_FADE_IN);
                    stay = ticks(t[1], DEFAULT_STAY);
                    fadeOut = ticks(t[2], DEFAULT_FADE_OUT);
                } catch (NumberFormatException ignored) {
                    fadeIn = DEFAULT_FADE_IN;
                    stay = DEFAULT_STAY;
                    fadeOut = DEFAULT_FADE_OUT;
                }
            } else if (segment.startsWith("actionbar:")) {
                screen().actionBar(player, body);
            } else if (segment.startsWith("message:")) {
                player.sendMessage(body);
            } else if (segment.startsWith("bossbar:")) {
                sendBossBar(player, strip(segment), ph);
            }
        }
        if (titleSeen) screen().title(player, title == null ? "" : title, subtitle, fadeIn, stay, fadeOut);
    }

    private static void sendBossBar(Player player, String spec, Map<String, String> ph) {
        String[] s = spec.split(":", -1);
        if (s.length < 4) return;
        try {
            if (!s[1].isEmpty()) Float.parseFloat(s[1]);
        } catch (NumberFormatException e) {
            return;
        }
        // Servers without org.bukkit.boss (1.8) show the text as an action bar instead.
        screen().actionBar(player, LegacyText.translate(s[0], ph));
    }

    /** Console and other non-player senders: chat and action bar text only; visual segments are skipped. */
    public static void send(CommandSender sender, String text, String... placeholders) {
        if (sender instanceof Player) {
            send((Player) sender, text, placeholders);
            return;
        }
        if (text == null || text.isEmpty()) return;
        Map<String, String> ph = pairs(placeholders);
        for (String segment : segments(text)) {
            if (segment.isEmpty()) continue;
            if (segment.startsWith("message:") || segment.startsWith("actionbar:")) {
                sender.sendMessage(LegacyText.translate(strip(segment), ph));
            } else if (!segment.startsWith("title:") && !segment.startsWith("subtitle:")
                    && !segment.startsWith("titletime:") && !segment.startsWith("bossbar:")) {
                sender.sendMessage(LegacyText.translate(segment, ph));
            }
        }
    }

    /** Plain chat message for code that has no prefix routing (admin command feedback). */
    public static void chat(CommandSender sender, String miniText, String... placeholders) {
        sender.sendMessage(LegacyText.translate(miniText, pairs(placeholders)));
    }

    private static final class ChatOnlyScreenText implements ScreenText {
        @Override
        public void title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
            player.sendMessage(subtitle == null ? title : title + " " + subtitle);
        }

        @Override
        public void actionBar(Player player, String text) {
            player.sendMessage(text);
        }
    }
}
