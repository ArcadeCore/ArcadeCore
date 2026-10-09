package org.drappula.arcadeApi.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegacyTextTest {
    private static final String S = "§";

    private static Map<String, String> ph(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    @Test
    void plainTextPassesThrough() {
        assertEquals("hello", LegacyText.translate("hello", Collections.<String, String>emptyMap()));
    }

    @Test
    void namedColorsBecomeSectionCodes() {
        assertEquals(S + "cstop", LegacyText.translate("<red>stop", Collections.<String, String>emptyMap()));
        assertEquals(S + "6gold", LegacyText.translate("<gold>gold", Collections.<String, String>emptyMap()));
        assertEquals(S + "8dim", LegacyText.translate("<dark_gray>dim", Collections.<String, String>emptyMap()));
    }

    @Test
    void closingTagRestoresOuterFormatting() {
        // <gold><b>X</b></gold> tail -> bold on, bold off (reset + gold), then reset on gold close
        String out = LegacyText.translate("<gold><b>X</b> y</gold> z", Collections.<String, String>emptyMap());
        assertEquals(S + "6" + S + "lX" + S + "r" + S + "6 y" + S + "r z", out);
    }

    @Test
    void placeholdersAreInsertedLiterally() {
        // value containing a tag must NOT be parsed (unparsed placeholder semantics)
        assertEquals("hi <red>bob", LegacyText.translate("hi <name>", ph("name", "<red>bob")));
    }

    @Test
    void unknownTagsStayAsText() {
        assertEquals("use <thing> here", LegacyText.translate("use <thing> here", Collections.<String, String>emptyMap()));
    }

    @Test
    void newlineTags() {
        assertEquals("a\nb\nc", LegacyText.translate("a<newline>b<br>c", Collections.<String, String>emptyMap()));
    }

    @Test
    void stripRemovesAllFormatting() {
        assertEquals("a b", LegacyText.strip(S + "6a " + S + "lb"));
    }
}
