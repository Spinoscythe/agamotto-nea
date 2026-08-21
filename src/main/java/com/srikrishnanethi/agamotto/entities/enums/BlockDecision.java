package com.srikrishnanethi.agamotto.entities.enums;

/**
 * Placement outcome stored in {@code schedule_blocks.decision}.
 */
public enum BlockDecision {
    /** Given a start/end inside the working window. */
    SCHEDULED,
    /** Kept in the plan but not given a slot (overflow remainder). */
    DELAYED,
    /** Dropped by Crunch best-fit so higher-priority work could fit. */
    EXCLUDED
}
