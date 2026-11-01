package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.SchedulePlan;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.TaskDependency;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.entities.enums.PlanStatus;
import com.srikrishnanethi.agamotto.entities.enums.TaskStatus;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import com.srikrishnanethi.agamotto.repositories.SchedulePlanRepository;
import com.srikrishnanethi.agamotto.repositories.TaskDependencyRepository;
import com.srikrishnanethi.agamotto.repositories.TaskRepository;
import com.srikrishnanethi.agamotto.repositories.UserProfileRepository;
import com.srikrishnanethi.agamotto.service.scheduler.GeneratedSchedule;
import com.srikrishnanethi.agamotto.service.scheduler.ScheduleResult;
import com.srikrishnanethi.agamotto.service.scheduler.SchedulerEngine;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

final class SchedulePlanGenerator {

    /**
     * Bit-vector set of statuses the engine is allowed to place.
     * {@link EnumSet} is a long-bitmask over {@link TaskStatus}, so {@code contains}
     * is O(1) and the set cannot hold a non-status value (unlike {@code HashSet<String>}).
     * COMPLETED / CANCELLED stay out so greedy never books finished work.
     */
    private static final EnumSet<TaskStatus> SCHEDULABLE = EnumSet.of(
            TaskStatus.PENDING,
            TaskStatus.IN_PROGRESS);

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final UserProfileRepository userProfileRepository;
    private final SchedulePlanRepository schedulePlanRepository;
    private final SchedulerEngine schedulerEngine;
    private final SchedulePlanNotifier schedulePlanNotifier;

    SchedulePlanGenerator(ProjectRepository projectRepository,
                          TaskRepository taskRepository,
                          TaskDependencyRepository taskDependencyRepository,
                          UserProfileRepository userProfileRepository,
                          SchedulePlanRepository schedulePlanRepository,
                          SchedulerEngine schedulerEngine,
                          SchedulePlanNotifier schedulePlanNotifier) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.taskDependencyRepository = taskDependencyRepository;
        this.userProfileRepository = userProfileRepository;
        this.schedulePlanRepository = schedulePlanRepository;
        this.schedulerEngine = schedulerEngine;
        this.schedulePlanNotifier = schedulePlanNotifier;
    }

    GeneratedSchedule generateAndPersist(String projectId, LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }

        // Project admin (creator) profile used for default availability
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        String ownerId = project.getOwner().getId();
        UserProfile profile = userProfileRepository.findByUserId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "UserProfile not found for project owner: " + ownerId));

        List<Task> tasks = taskRepository.findByProjectIdAndStatusIn(projectId, SCHEDULABLE);
        List<TaskDependency> dependencyEdges = taskDependencyRepository.findByProjectId(projectId);

        ScheduleResult result = schedulerEngine.generate(tasks, startDate, endDate, profile, dependencyEdges);

        archiveActivePlans(projectId);

        SchedulePlan plan = new SchedulePlan();
        plan.setProject(project);
        plan.setMode(result.mode());
        plan.setStatus(PlanStatus.ACTIVE);
        plan.setStartDate(startDate);
        plan.setEndDate(endDate);
        plan.setGeneratedAt(Instant.now());
        plan.setExplanationSummary(result.explanationSummary());

        for (ScheduleBlock block : result.blocks()) {
            block.setSchedule(plan);
            plan.getBlocks().add(block);
        }

        SchedulePlan saved = schedulePlanRepository.save(plan);
        SchedulePlanGraphInitializer.initializePlanGraph(saved);
        schedulePlanNotifier.notifyOwner(project, saved);
        return new GeneratedSchedule(saved, result.explanationSummary());
    }

    private void archiveActivePlans(String projectId) {
        List<SchedulePlan> existing = schedulePlanRepository.findByProjectIdOrderByGeneratedAtDesc(projectId);
        for (SchedulePlan plan : existing) {
            if (plan.getStatus() == PlanStatus.ACTIVE) {
                plan.setStatus(PlanStatus.ARCHIVED);
                schedulePlanRepository.save(plan);
            }
        }
    }
}
