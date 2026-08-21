package com.srikrishnanethi.agamotto.entities.enums;

/**
 * Kind stored in {@code notifications.type}.
 */
public enum NotificationType {
    /** Generic or task-linked message. */
    GENERAL,
    /** Deadline reminder (reserved). */
    DEADLINE,
    /** Schedule was generated or a task was delayed/excluded. */
    SCHEDULE,
    /** Invitation to join a project. */
    PROJECT_INVITE
}
