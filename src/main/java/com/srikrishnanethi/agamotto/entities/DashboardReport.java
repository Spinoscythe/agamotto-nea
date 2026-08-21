package com.srikrishnanethi.agamotto.entities;

import com.srikrishnanethi.agamotto.entities.enums.ReportPeriod;
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
import java.time.LocalDate;

/**
 * Snapshot of schedule/task counts for a reporting window. Maps to {@code dashboard_reports}.
 */
@Entity
@Table(name = "dashboard_reports")
@Comment("Persisted overview counts for a user over a daily, weekly, or monthly window")
public class DashboardReport extends BaseEntity {

    @Comment("User the counts belong to (FK users.user_id)")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Comment("DAILY, WEEKLY, or MONTHLY")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReportPeriod period;

    @Comment("Inclusive start date of the window")
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Comment("Inclusive end date of the window (usually 'as of')")
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Comment("How many SCHEDULED blocks fell in the window")
    @Column(name = "scheduled_count", nullable = false)
    private int scheduledCount;

    @Comment("How many DELAYED blocks fell in the window")
    @Column(name = "delayed_count", nullable = false)
    private int delayedCount;

    @Comment("How many EXCLUDED blocks fell in the window")
    @Column(name = "excluded_count", nullable = false)
    private int excludedCount;

    @Comment("How many tasks were marked COMPLETED in the window")
    @Column(name = "completed_count", nullable = false)
    private int completedCount;

    @Comment("When this snapshot was written")
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ReportPeriod getPeriod() {
        return period;
    }

    public void setPeriod(ReportPeriod period) {
        this.period = period;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public int getScheduledCount() {
        return scheduledCount;
    }

    public void setScheduledCount(int scheduledCount) {
        this.scheduledCount = scheduledCount;
    }

    public int getDelayedCount() {
        return delayedCount;
    }

    public void setDelayedCount(int delayedCount) {
        this.delayedCount = delayedCount;
    }

    public int getExcludedCount() {
        return excludedCount;
    }

    public void setExcludedCount(int excludedCount) {
        this.excludedCount = excludedCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
