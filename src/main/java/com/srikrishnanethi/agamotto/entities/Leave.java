package com.srikrishnanethi.agamotto.entities;

import com.srikrishnanethi.agamotto.entities.enums.LeaveType;
import jakarta.persistence.AttributeOverride;
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
import java.time.LocalDateTime;

/**
 * User scheduled leave, personal break, or global holiday.
 * Maps to table {@code user_leaves}.
 */
@Entity
@Table(name = "user_leaves")
@Comment("Scheduled leaves, breaks, and holidays where work cannot be scheduled")
@AttributeOverride(name = "id", column = @Column(name = "leave_id", length = 36, nullable = false, updatable = false))
public class Leave extends BaseEntity {

    @Comment("Owning user; null indicates a system-wide or global break/holiday")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Comment("Title or short label for the leave/break")
    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Comment("Type of leave (VACATION, SICK_LEAVE, SHORT_BREAK, PERSONAL, HOLIDAY)")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private LeaveType type;

    @Comment("Start date and time of the leave")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Comment("End date and time of the leave")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Comment("Optional explanation or note")
    @Column(name = "reason", length = 1000)
    private String reason;

    @Comment("When this leave was created")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Leave() {
    }

    public Leave(User user, String title, LeaveType type, LocalDateTime startTime, LocalDateTime endTime,
            String reason) {
        this.user = user;
        this.title = title;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LeaveType getType() {
        return type;
    }

    public void setType(LeaveType type) {
        this.type = type;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isGlobal() {
        return this.user == null;
    }
}
