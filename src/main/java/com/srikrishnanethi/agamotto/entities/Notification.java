package com.srikrishnanethi.agamotto.entities;

import com.srikrishnanethi.agamotto.entities.enums.NotificationType;
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
 * Inbox row for a user. Maps to table {@code notifications}.
 */
@Entity
@Table(name = "notifications")
@Comment("Unread/read alerts for schedules, deadlines, and project invites")
public class Notification extends BaseEntity {

    @Comment("Recipient (FK users.user_id)")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Comment("Related task if this alert is about a task; null otherwise")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @Comment("Related project if this alert is about a project or invite")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Comment("Related invite when type is PROJECT_INVITE")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invite_id")
    private ProjectInvite invite;

    @Comment("GENERAL, DEADLINE, SCHEDULE, or PROJECT_INVITE")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type = NotificationType.GENERAL;

    @Comment("Text shown in the notifications list")
    @Column(nullable = false, length = 500)
    private String message;

    @Comment("False until the user marks it read or accepts/declines an invite")
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Comment("When the alert was created")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Comment("When the alert was considered sent; may match created_at")
    @Column(name = "sent_at")
    private Instant sentAt;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public ProjectInvite getInvite() {
        return invite;
    }

    public void setInvite(ProjectInvite invite) {
        this.invite = invite;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
