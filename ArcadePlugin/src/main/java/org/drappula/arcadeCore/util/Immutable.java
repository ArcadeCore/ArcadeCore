package org.drappula.arcadeCore.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Java 8 stand-in for {@code List.copyOf}: an unmodifiable snapshot that later changes to the source do not touch. */
public final class Immutable {
    private Immutable() {
    }

    public static <T> List<T> copy(Collection<? extends T> source) {
        return Collections.unmodifiableList(new ArrayList<T>(source));
    }
}
