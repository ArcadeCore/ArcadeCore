package org.drappula.arcadeApi.systems.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapConfigOptionTest {

    @Test
    void integerAcceptsInRangeAndCanonicalizes() {
        MapConfigOption option = MapConfigOption.integer("lava-blocks-rise-per-minute", 5, 0, 60);

        assertEquals("5", option.validateAndCanonicalize("5"));
        assertEquals("7", option.validateAndCanonicalize("007"));
        assertEquals("5", option.defaultValue());
        assertEquals(MapConfigType.INTEGER, option.type());
    }

    @Test
    void integerRejectsOutOfRangeAndNonNumeric() {
        MapConfigOption option = MapConfigOption.integer("speed", 5, 0, 60);

        assertThrows(IllegalArgumentException.class, () -> option.validateAndCanonicalize("61"));
        assertThrows(IllegalArgumentException.class, () -> option.validateAndCanonicalize("-1"));
        assertThrows(IllegalArgumentException.class, () -> option.validateAndCanonicalize("fast"));
    }

    @Test
    void decimalAcceptsFractionsAndCanonicalizes() {
        MapConfigOption option = MapConfigOption.decimal("rise-rate", 1.5, 0.0, 10.0);

        assertEquals("2.5", option.validateAndCanonicalize("2.50"));
        assertThrows(IllegalArgumentException.class, () -> option.validateAndCanonicalize("abc"));
        assertThrows(IllegalArgumentException.class, () -> option.validateAndCanonicalize("10.1"));
    }

    @Test
    void booleanAcceptsOnlyTrueFalse() {
        MapConfigOption option = MapConfigOption.booleanOption("sudden-death", false);

        assertEquals("true", option.validateAndCanonicalize("True"));
        assertEquals("false", option.validateAndCanonicalize("FALSE"));
        assertThrows(IllegalArgumentException.class, () -> option.validateAndCanonicalize("yes"));
        assertThrows(IllegalArgumentException.class, () -> option.validateAndCanonicalize(""));
    }

    @Test
    void textPassesThroughUnchanged() {
        MapConfigOption option = MapConfigOption.text("motd", "hello");

        assertEquals("hello world", option.validateAndCanonicalize("hello world"));
        assertEquals("", option.validateAndCanonicalize(""));
    }

    @Test
    void keyFormatIsEnforced() {
        assertDoesNotThrow(() -> MapConfigOption.integer("a-b-0", 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> MapConfigOption.integer("", 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> MapConfigOption.integer("Bad Key", 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> MapConfigOption.integer("UPPER", 0, 0, 1));
    }

    @Test
    void invalidDefaultRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> MapConfigOption.integer("speed", 99, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> MapConfigOption.booleanOption("flag", true).validateAndCanonicalize("maybe"));
    }
}
