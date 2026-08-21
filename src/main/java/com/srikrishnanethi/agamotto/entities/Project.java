package com.srikrishnanethi.agamotto.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A container for tasks and generated schedules. Maps to table {@code projects}.
 */
@Entity
@Table(name = "projects")
@Comment("Work container owned by one user and optionally shared with members")
public class Project extends BaseEntity {

    @Comment("Account that created and owns this project (FK users.user_id)")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Comment("Short project title shown in lists")
    @Column(nullable = false, length = 200)
    private String name;

    @Comment("Optional longer description")
    @Column(length = 2000)
    private String description;

    @Comment("Inclusive planned start date")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Comment("Inclusive planned end date; must be on or after start_date")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Comment("Rough total effort the owner expects, in hours")
    @Column(name = "estimated_effort_hours", nullable = false)
    private double estimatedEffortHours;

    @Comment("When this project row was created")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "project")
    private List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "project")
    private List<SchedulePlan> schedulePlans = new ArrayList<>();

    public List<SchedulePlan> getSchedulePlans() {
        return schedulePlans;
    }

    public void setSchedulePlans(List<SchedulePlan> schedulePlans) {
        this.schedulePlans = schedulePlans;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public double getEstimatedEffortHours() {
        return estimatedEffortHours;
    }

    public void setEstimatedEffortHours(double estimatedEffortHours) {
        this.estimatedEffortHours = estimatedEffortHours;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
