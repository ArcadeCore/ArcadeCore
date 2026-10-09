package org.drappula.arcadeCore.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.drappula.arcadeCore.ArcadeCore;

/**
 * Logging that works on every server version: java.util.logging with SLF4J-style {@code {}} placeholders.
 * A trailing Throwable that no placeholder consumed is attached to the record.
 */
public final class Log {
    private Log() {
    }

    private static Logger logger() {
        ArcadeCore core = ArcadeCore.get();
        Logger logger = core == null ? null : core.getLogger();
        return logger != null ? logger : Logger.getLogger("ArcadeCore");
    }

    public static void warn(String format, Object... args) {
        log(Level.WARNING, format, args);
    }

    public static void error(String format, Object... args) {
        log(Level.SEVERE, format, args);
    }

    static String format(String format, Object[] args) {
        StringBuilder out = new StringBuilder();
        int arg = 0;
        int i = 0;
        while (i < format.length()) {
            if (format.startsWith("{}", i) && arg < args.length) {
                out.append(args[arg++]);
                i += 2;
            } else {
                out.append(format.charAt(i++));
            }
        }
        return out.toString();
    }

    private static void log(Level level, String format, Object[] args) {
        int placeholders = 0;
        for (int i = format.indexOf("{}"); i >= 0; i = format.indexOf("{}", i + 2)) placeholders++;
        Throwable thrown = null;
        if (args.length > placeholders && args[args.length - 1] instanceof Throwable) {
            thrown = (Throwable) args[args.length - 1];
        }
        logger().log(level, format(format, args), thrown);
    }
}
