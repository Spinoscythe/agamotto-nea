package com.srikrishnanethi.agamotto.entities.enums;

/**
 * Plan lifecycle stored in {@code schedule_plans.status}.
 */
public enum PlanStatus {
    /** Unused placeholder; generation writes ACTIVE. */
    DRAFT,
    /** Current timetable for the project. */
    ACTIVE,
    /** Replaced by a newer generation. */
    ARCHIVED
}
