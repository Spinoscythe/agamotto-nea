package com.srikrishnanethi.agamotto.entities.enums;

/**
 * Invite lifecycle stored in {@code project_invites.status}.
 */
public enum InviteStatus {
    /** Waiting for the invitee. */
    PENDING,
    /** Invitee joined the project. */
    ACCEPTED,
    /** Invitee refused. */
    DECLINED,
    /** Owner cancelled before a response. */
    CANCELLED
}
