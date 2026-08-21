package com.srikrishnanethi.agamotto.entities.enums;

/**
 * Collaboration role stored in {@code project_members.role} and {@code project_invites.role}.
 */
public enum ProjectRole {
    /** Created the project; can invite, remove members, and delete. */
    OWNER,
    /** Can create and change tasks and schedules. */
    EDITOR,
    /** Can read the project only. */
    VIEWER;

    public boolean canView() {
        return true;
    }

    public boolean canEdit() {
        return this == OWNER || this == EDITOR;
    }

    public boolean canManageMembers() {
        return this == OWNER;
    }
}
