package com.srikrishnanethi.agamotto.entities.enums;

/**
 * Engine mode stored in {@code schedule_plans.mode}.
 */
public enum ScheduleMode {
    /** Workload fitted available hours; greedy placement, no exclusions. */
    SERENITY,
    /** Over capacity; best-fit drop then greedy place. */
    CRUNCH
}
