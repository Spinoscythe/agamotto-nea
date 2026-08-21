package com.srikrishnanethi.agamotto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.time.LocalTime;

/**
 * Per-user scheduler preferences. Maps to table {@code user_profiles}.
 */
@Entity
@Table(name = "user_profiles")
@Comment("Working hours and scoring weights used by Serenity/Crunch")
public class UserProfile extends BaseEntity {

    @Comment("Owning user (one profile per account)")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Comment("Local time when a work day starts (default 09:00)")
    @Column(name = "preferred_start", nullable = false)
    private LocalTime preferredStart = LocalTime.of(9, 0);

    @Comment("Local time when a work day ends (default 17:00)")
    @Column(name = "preferred_end", nullable = false)
    private LocalTime preferredEnd = LocalTime.of(17, 0);

    @Comment("Whether Saturday and Sunday are available for placement")
    @Column(name = "include_weekends", nullable = false)
    private boolean includeWeekends = true;

    @Comment("Weight w_p: how strongly task priority affects scoreTask")
    @Column(name = "weight_priority", nullable = false)
    private double weightPriority = 1.0;

    @Comment("Weight w_u: how strongly deadline urgency affects scoreTask")
    @Column(name = "weight_urgency", nullable = false)
    private double weightUrgency = 1.0;

    @Comment("Weight w_ed: how strongly estimated duration affects scoreTask")
    @Column(name = "weight_duration", nullable = false)
    private double weightDuration = 1.0;

    @Comment("When these preferences were last saved")
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
