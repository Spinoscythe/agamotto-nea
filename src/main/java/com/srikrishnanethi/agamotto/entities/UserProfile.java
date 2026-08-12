package com.srikrishnanethi.agamotto.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "preferred_start", nullable = false)
    private LocalTime preferredStart = LocalTime.of(9, 0);

    @Column(name = "preferred_end", nullable = false)
    private LocalTime preferredEnd = LocalTime.of(17, 0);

    @Column(name = "include_weekends", nullable = false)
    private boolean includeWeekends = true;

    /** Weight for priority in scoreTask (w_p). Kept for scheduler */
    @Column(name = "weight_priority", nullable = false)
    private double weightPriority = 1.0;

    /** Weight for deadline urgency in scoreTask (w_u). */
    @Column(name = "weight_urgency", nullable = false)
    private double weightUrgency = 1.0;

    /** Weight for estimated duration in scoreTask (w_ed). */
    @Column(name = "weight_duration", nullable = false)
    private double weightDuration = 1.0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalTime getPreferredStart() {
        return preferredStart;
    }

    public void setPreferredStart(LocalTime preferredStart) {
        this.preferredStart = preferredStart;
    }

    public LocalTime getPreferredEnd() {
        return preferredEnd;
    }

    public void setPreferredEnd(LocalTime preferredEnd) {
        this.preferredEnd = preferredEnd;
    }

    public boolean isIncludeWeekends() {
        return includeWeekends;
    }

    public void setIncludeWeekends(boolean includeWeekends) {
        this.includeWeekends = includeWeekends;
    }

    public double getWeightPriority() {
        return weightPriority;
    }

    public void setWeightPriority(double weightPriority) {
        this.weightPriority = weightPriority;
    }

    public double getWeightUrgency() {
        return weightUrgency;
    }

    public void setWeightUrgency(double weightUrgency) {
        this.weightUrgency = weightUrgency;
    }

    public double getWeightDuration() {
        return weightDuration;
    }

    public void setWeightDuration(double weightDuration) {
        this.weightDuration = weightDuration;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
