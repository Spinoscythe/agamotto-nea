package com.srikrishnanethi.agamotto.entities;

import com.srikrishnanethi.agamotto.entities.enums.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Work item that the scheduler places into blocks. Maps to table {@code tasks}.
 */
@Entity
@Table(name = "tasks")
@Comment("Work items belonging to a project, with deadline, effort, and status")
public class Task extends BaseEntity {

    @Comment("Parent project (FK projects.id)")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Comment("Short task name")
    @Column(nullable = false, length = 200)
    private String title;

    @Comment("Optional details or project-type note")
    @Column(length = 2000)
    private String description;

    @Comment("Free-text category such as work or revision")
    @Column(nullable = false, length = 100)
    private String category;

    @Comment("Importance 1-5; higher is placed sooner")
    @Column(nullable = false)
    private int priority;

    @Comment("When the task must be finished")
    @Column(nullable = false)
    private LocalDateTime deadline;

    @Comment("Owner's duration estimate in hours")
    @Column(name = "estimated_duration_hours", nullable = false)
    private double estimatedDurationHours;

    @Comment("Optional corrected duration after the user adjusts an estimate; null if unused")
    @Column(name = "corrected_duration_hours")
    private Double correctedDurationHours;

    @Comment("Cognitive difficulty 1-5; used to split Serenity sessions")
    @Column(nullable = false)
    private int complexity;

    @Comment("Lifecycle: PENDING, IN_PROGRESS, COMPLETED, CANCELLED")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskStatus status = TaskStatus.PENDING;

    @Comment("When the task row was inserted")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Comment("When title, status, or estimates last changed")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "task")
    private List<ScheduleBlock> scheduleBlocks = new ArrayList<>();

    @OneToMany(mappedBy = "task")
    private List<TaskHistory> history = new ArrayList<>();

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public double getEstimatedDurationHours() {
        return estimatedDurationHours;
    }

    public void setEstimatedDurationHours(double estimatedDurationHours) {
        this.estimatedDurationHours = estimatedDurationHours;
    }

    public Double getCorrectedDurationHours() {
        return correctedDurationHours;
    }

    public void setCorrectedDurationHours(Double correctedDurationHours) {
        this.correctedDurationHours = correctedDurationHours;
    }

    public int getComplexity() {
        return complexity;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ScheduleBlock> getScheduleBlocks() {
        return scheduleBlocks;
    }

    public void setScheduleBlocks(List<ScheduleBlock> scheduleBlocks) {
        this.scheduleBlocks = scheduleBlocks;
    }

    public List<TaskHistory> getHistory() {
        return history;
    }

    public void setHistory(List<TaskHistory> history) {
        this.history = history;
    }
}
