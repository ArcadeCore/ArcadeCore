package org.drappula.arcadeApi.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Translates the small MiniMessage subset ArcadeCore uses (named and hex colours, bold/italic/underlined/
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

    /** The 16 legacy colours as RGB, for mapping hex colours to the nearest one. */
    private static final int[] RGB = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
    private static final String CODES = "0123456789abcdef";

    /** Colour code for a tag name: a named colour, {@code #rrggbb}, or {@code color:<either>}; 0 if it is not one. */
    private static char colour(String name) {
        if (name.startsWith("color:")) name = name.substring(6);
        else if (name.startsWith("colour:")) name = name.substring(7);
        if (name.length() == 7 && name.charAt(0) == '#') {
            try {
                int rgb = Integer.parseInt(name.substring(1), 16);
                int best = 0;
                long bestDistance = Long.MAX_VALUE;
                for (int k = 0; k < RGB.length; k++) {
                    long dr = ((rgb >> 16) & 255) - ((RGB[k] >> 16) & 255);
                    long dg = ((rgb >> 8) & 255) - ((RGB[k] >> 8) & 255);
                    long db = (rgb & 255) - (RGB[k] & 255);
                    long distance = dr * dr + dg * dg + db * db;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = k;
                    }
                }
                return CODES.charAt(best);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return colorCode(name);
    }

    /** Colour and formats currently in effect. Legacy codes cannot be undone one by one, so this is re-emitted. */
    private static final class State {
        char colour;
        final StringBuilder formats = new StringBuilder();

        void emit(StringBuilder out) {
            out.append(S).append('r');
            if (colour != 0) out.append(S).append(colour);
            for (int k = 0; k < formats.length(); k++) out.append(S).append(formats.charAt(k));
        }
    }

    public static String translate(String text, Map<String, String> placeholders) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        // One entry per open tag, innermost last: the code it applied (colour or format).
        List<String> open = new ArrayList<String>();
        State state = new State();
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
                open.clear();
                state = new State();
                out.append(S).append('r');
            } else if (!closing && colour(name) != 0) {
                state.colour = colour(name);
                open.add(name);
                // A colour code wipes formats in legacy text, so put the active formats back after it.
                out.append(S).append(state.colour);
                for (int k = 0; k < state.formats.length(); k++) out.append(S).append(state.formats.charAt(k));
            } else if (!closing && formatCode(name) != 0) {
                char code = formatCode(name);
                if (state.formats.indexOf(String.valueOf(code)) < 0) state.formats.append(code);
                open.add(name);
                out.append(S).append(code);
            } else if (closing && (colour(name) != 0 || formatCode(name) != 0 || name.equals("color") || name.equals("colour"))) {
                if (!popTag(open, name)) {
                    // Closing something that was never opened changes nothing.
                } else {
                    state = rebuild(open);
                    state.emit(out);
                }
            } else {
                out.append(c);
                i++;
                continue;
            }
            i = end + 1;
        }
        return out.toString();
    }

    /** Removes the innermost open tag that the closing tag matches. */
    private static boolean popTag(List<String> open, String closing) {
        for (int k = open.size() - 1; k >= 0; k--) {
            String opened = open.get(k);
            boolean same = opened.equals(closing)
                    || ((closing.equals("color") || closing.equals("colour")) && (opened.startsWith("color:") || opened.startsWith("colour:")))
                    || (opened.startsWith("color:") && opened.substring(6).equals(closing))
                    || (opened.startsWith("colour:") && opened.substring(7).equals(closing));
            if (same) {
                open.remove(k);
                return true;
            }
        }
        return false;
    }

    private static State rebuild(List<String> open) {
        State state = new State();
        for (String name : open) {
            char colourCode = colour(name);
            if (colourCode != 0) {
                state.colour = colourCode;
            } else if (state.formats.indexOf(String.valueOf(formatCode(name))) < 0) {
                state.formats.append(formatCode(name));
            }
        }
        return state;
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
