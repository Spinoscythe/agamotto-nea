package com.srikrishnanethi.agamotto.entities;

import com.srikrishnanethi.agamotto.entities.enums.InviteStatus;
import com.srikrishnanethi.agamotto.entities.enums.ProjectRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.Instant;

/**
 * Invitation for a registered email to join a project. Maps to {@code project_invites}.
 */
@Entity
@Table(name = "project_invites")
@Comment("Pending or resolved invitations to join a project as editor or viewer")
public class ProjectInvite extends BaseEntity {

    @Comment("Project being shared (FK projects.id)")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Comment("Owner who sent the invite (FK users.user_id)")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @Comment("Email of the registered user being invited")
    @Column(name = "invitee_email", nullable = false, length = 255)
    private String inviteeEmail;

    @Comment("Role they will receive: EDITOR or VIEWER (never OWNER)")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectRole role;

    @Comment("PENDING, ACCEPTED, DECLINED, or CANCELLED")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InviteStatus status = InviteStatus.PENDING;

    @Comment("When the invite was created")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Comment("When it was accepted, declined, or cancelled; null while pending")
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public User getInviter() {
        return inviter;
    }

    public void setInviter(User inviter) {
        this.inviter = inviter;
    }

    public String getInviteeEmail() {
        return inviteeEmail;
    }

    public void setInviteeEmail(String inviteeEmail) {
        this.inviteeEmail = inviteeEmail;
    }

    public ProjectRole getRole() {
        return role;
    }

    public void setRole(ProjectRole role) {
        this.role = role;
    }

    public InviteStatus getStatus() {
        return status;
    }

    public void setStatus(InviteStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
