package com.srikrishnanethi.agamotto.entities;

import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * One placement decision for a task on a plan. Maps to table {@code schedule_blocks}.
 */
@Entity
@Table(name = "schedule_blocks")
@Comment("Individual scheduled, delayed, or excluded slot produced by the engine")
public class ScheduleBlock extends BaseEntity {

    @Comment("Parent plan (FK schedule_plans.id)")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private SchedulePlan schedule;

    @Comment("Task this slot is about (FK tasks.id)")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Comment("Slot start; null when DELAYED or EXCLUDED")
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Comment("Slot end; null when DELAYED or EXCLUDED")
    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Comment("SCHEDULED, DELAYED, or EXCLUDED")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BlockDecision decision;

    @Comment("Why the engine (or a later override) chose this decision")
    @Column(nullable = false, length = 1000)
    private String reason;

    @Comment("True after a user moved, delayed, or otherwise overrode the engine")
    @Column(name = "manually_overridden", nullable = false)
    private boolean manuallyOverridden = false;

    public SchedulePlan getSchedule() {
        return schedule;
    }

    public void setSchedule(SchedulePlan schedule) {
        this.schedule = schedule;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
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

    public BlockDecision getDecision() {
        return decision;
    }

    public void setDecision(BlockDecision decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isManuallyOverridden() {
        return manuallyOverridden;
    }

    public void setManuallyOverridden(boolean manuallyOverridden) {
        this.manuallyOverridden = manuallyOverridden;
    }
}
