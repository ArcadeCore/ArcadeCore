package org.drappula.arcadeApi.systems.queue;

/** Outcome of a queue join attempt. Replaces the old boolean so addons can explain failures. */
public enum JoinResult {
    SUCCESS,
    ALREADY_QUEUED,
    ALREADY_IN_MATCH,
    EVENT_DENIED,
    GAME_DISABLED
}
