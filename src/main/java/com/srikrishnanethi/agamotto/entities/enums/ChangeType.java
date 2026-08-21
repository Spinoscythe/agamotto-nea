package com.srikrishnanethi.agamotto.entities.enums;

/**
 * Audit action stored in {@code task_history.change_type}.
 */
public enum ChangeType {
    /** First insert of the task row. */
    CREATED,
    /** Field edits other than status. */
    EDITED,
    /** Status moved between PENDING / IN_PROGRESS / COMPLETED / CANCELLED. */
    STATUS_CHANGED,
    /** Soft-delete / remove recorded in history. */
    DELETED
}
