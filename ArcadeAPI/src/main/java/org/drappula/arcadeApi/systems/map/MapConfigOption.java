package org.drappula.arcadeApi.systems.map;

import java.util.regex.Pattern;

/**
 * A typed per-map tuning knob declared by an addon game. Admins set values
 * per map via {@code /arcade map config}; the core validates input with
 * {@link #validateAndCanonicalize(String)} and addons read the effective
 * value (override or default) from their match's map.
 */
public final class MapConfigOption {
    private static final Pattern KEY = Pattern.compile("[a-z0-9-]+");

    private final String key;
    private final MapConfigType type;
    private final String defaultValue;
    private final double min;
    private final double max;

    private MapConfigOption(String key, MapConfigType type, String defaultValue, double min, double max) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Map config key must match [a-z0-9-]+: " + key);
        }
        this.key = key;
        this.type = type;
        this.min = min;
        this.max = max;
        this.defaultValue = validateAndCanonicalize(defaultValue);
    }

    public static MapConfigOption integer(String key, int defaultValue, int min, int max) {
        if (min > max) throw new IllegalArgumentException("min must be <= max for " + key);
        return new MapConfigOption(key, MapConfigType.INTEGER, Integer.toString(defaultValue), min, max);
    }

    public static MapConfigOption decimal(String key, double defaultValue, double min, double max) {
        if (min > max) throw new IllegalArgumentException("min must be <= max for " + key);
        return new MapConfigOption(key, MapConfigType.DECIMAL, Double.toString(defaultValue), min, max);
    }

    public static MapConfigOption booleanOption(String key, boolean defaultValue) {
        return new MapConfigOption(key, MapConfigType.BOOLEAN, Boolean.toString(defaultValue), 0, 0);
    }

    public static MapConfigOption text(String key, String defaultValue) {
        return new MapConfigOption(key, MapConfigType.TEXT, defaultValue == null ? "" : defaultValue, 0, 0);
    }

    public String key() {
        return key;
    }

    public MapConfigType type() {
        return type;
    }

    /** The canonical default value (validated at construction). */
    public String defaultValue() {
        return defaultValue;
    }

    /**
     * Parses and range-checks raw input, returning the canonical string to
     * store. Throws {@code IllegalArgumentException} with a human reason when
     * the value is unusable.
     */
    public String validateAndCanonicalize(String raw) {
        if (type == MapConfigType.TEXT) {
            return raw == null ? "" : raw;
        }
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException(key + " requires a " + describeType() + ", got blank");
        }
        String trimmed = raw.trim();
        switch (type) {
            case INTEGER: {
                int value;
                try {
                    value = Integer.parseInt(trimmed);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(key + " requires a whole number, got '" + raw + "'");
                }
                if (value < min || value > max) {
                    throw new IllegalArgumentException(
                            key + " must be between " + (int) min + " and " + (int) max + ", got " + value);
                }
                return Integer.toString(value);
            }
            case DECIMAL: {
                double value;
                try {
                    value = Double.parseDouble(trimmed);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(key + " requires a number, got '" + raw + "'");
                }
                if (value < min || value > max) {
                    throw new IllegalArgumentException(
                            key + " must be between " + min + " and " + max + ", got " + value);
                }
                return Double.toString(value);
            }
            case BOOLEAN: {
                if (trimmed.equalsIgnoreCase("true")) return "true";
                if (trimmed.equalsIgnoreCase("false")) return "false";
                throw new IllegalArgumentException(key + " requires true or false, got '" + raw + "'");
            }
            default:
                throw new IllegalArgumentException("Unknown type for " + key);
        }
    }

    private String describeType() {
        switch (type) {
            case INTEGER: return "whole number between " + (int) min + " and " + (int) max;
            case DECIMAL: return "number between " + min + " and " + max;
            case BOOLEAN: return "true or false";
            default: return "text";
        }
    }
}
