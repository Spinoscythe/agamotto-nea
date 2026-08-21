package com.srikrishnanethi.agamotto.entities.enums;

/**
 * Lifecycle stored in {@code tasks.status}.
 */
public enum TaskStatus {
    /** Not started; eligible for scheduling. */
    PENDING,
    /** Currently being worked on; still eligible for scheduling. */
    IN_PROGRESS,
    /** Finished; counted on the dashboard, not placed again. */
    COMPLETED,
    /** Abandoned; not placed and not counted as completed. */
    CANCELLED
}
