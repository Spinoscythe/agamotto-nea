package com.srikrishnanethi.agamotto.entities;

import com.srikrishnanethi.agamotto.entities.enums.PlanStatus;
import com.srikrishnanethi.agamotto.entities.enums.ScheduleMode;
import jakarta.persistence.CascadeType;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * One generated timetable for a project. Maps to table {@code schedule_plans}.
 */
@Entity
@Table(name = "schedule_plans")
@Comment("Generated timetable header: mode, window, and explanation")
public class SchedulePlan extends BaseEntity {

    @Comment("Project this plan belongs to (FK projects.id)")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Comment("SERENITY if work fitted capacity, otherwise CRUNCH")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ScheduleMode mode;

    @Comment("DRAFT, ACTIVE, or ARCHIVED; generating a new plan archives the previous ACTIVE one")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlanStatus status = PlanStatus.DRAFT;

    @Comment("First day of the placement window")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Comment("Last day of the placement window")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Comment("When the engine produced this plan")
    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    @Comment("Short engine explanation shown in the UI")
    @Column(name = "explanation_summary", length = 1024)
    private String explanationSummary;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleBlock> blocks = new ArrayList<>();

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public ScheduleMode getMode() {
        return mode;
    }

    public void setMode(ScheduleMode mode) {
        this.mode = mode;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getExplanationSummary() {
        return explanationSummary;
    }

    public void setExplanationSummary(String explanationSummary) {
        this.explanationSummary = explanationSummary;
    }

    public List<ScheduleBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<ScheduleBlock> blocks) {
        this.blocks = blocks;
    }
}
