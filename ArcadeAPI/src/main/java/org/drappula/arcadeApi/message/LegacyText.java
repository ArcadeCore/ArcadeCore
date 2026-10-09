package org.drappula.arcadeApi.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Translates the small MiniMessage subset ArcadeCore uses (named colours, bold/italic/underlined/
 * strikethrough/obfuscated, reset, newline) into legacy section-sign codes, so one message string works
 * on every Minecraft version. Anything else in angle brackets is left as text.
 */
public final class LegacyText {
    private static final char S = '§';

    private LegacyText() {
    }

    private static char colorCode(String name) {
        switch (name) {
            case "black": return '0';
            case "dark_blue": return '1';
            case "dark_green": return '2';
            case "dark_aqua": return '3';
            case "dark_red": return '4';
            case "dark_purple": return '5';
            case "gold": return '6';
            case "gray": case "grey": return '7';
            case "dark_gray": case "dark_grey": return '8';
            case "blue": return '9';
            case "green": return 'a';
            case "aqua": return 'b';
            case "red": return 'c';
            case "light_purple": return 'd';
            case "yellow": return 'e';
            case "white": return 'f';
            default: return 0;
        }
    }

    private static char formatCode(String name) {
        switch (name) {
            case "b": case "bold": return 'l';
            case "i": case "em": case "italic": return 'o';
            case "u": case "underlined": return 'n';
            case "st": case "strikethrough": return 'm';
            case "obf": case "obfuscated": return 'k';
            default: return 0;
        }
    }

    /** One open tag: either a colour (code) or a format (code). */
    private static final class Open {
        final String name;
        final char code;
        final boolean color;

        Open(String name, char code, boolean color) {
            this.name = name;
            this.code = code;
            this.color = color;
        }
    }

    public static String translate(String text, Map<String, String> placeholders) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        List<Open> stack = new ArrayList<Open>();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            int end = c == '<' ? text.indexOf('>', i) : -1;
            if (end < 0) {
                out.append(c);
                i++;
                continue;
            }
            String tag = text.substring(i + 1, end);
            boolean closing = tag.startsWith("/");
            String name = closing ? tag.substring(1) : tag;
            if (!closing && placeholders.containsKey(name)) {
                out.append(placeholders.get(name));
            } else if (name.equals("newline") || name.equals("br")) {
                out.append('\n');
            } else if (name.equals("reset")) {
                stack.clear();
                out.append(S).append('r');
            } else if (!closing && colorCode(name) != 0) {
                stack.add(new Open(name, colorCode(name), true));
                out.append(S).append(colorCode(name));
            } else if (!closing && formatCode(name) != 0) {
                stack.add(new Open(name, formatCode(name), false));
                out.append(S).append(formatCode(name));
            } else if (closing && (colorCode(name) != 0 || formatCode(name) != 0)) {
                for (int k = stack.size() - 1; k >= 0; k--) {
                    if (stack.get(k).name.equals(name)) {
                        stack.remove(k);
                        break;
                    }
                }
                // Legacy codes cannot be undone individually: reset, then re-apply what is still open.
                out.append(S).append('r');
                for (Open open : stack) out.append(S).append(open.code);
            } else {
                out.append(c);
                i++;
                continue;
            }
            i = end + 1;
        }
        return out.toString();
    }

    /** Removes legacy colour/format codes, leaving plain text. */
    public static String strip(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == S && i + 1 < text.length()) {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }
}
