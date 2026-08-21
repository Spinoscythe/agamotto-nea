package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.SchedulePlan;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import com.srikrishnanethi.agamotto.entities.enums.PlanStatus;
import com.srikrishnanethi.agamotto.entities.enums.TaskStatus;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.exception.ScheduleConflictException;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import com.srikrishnanethi.agamotto.repositories.ScheduleBlockRepository;
import com.srikrishnanethi.agamotto.repositories.SchedulePlanRepository;
import com.srikrishnanethi.agamotto.repositories.TaskRepository;
import com.srikrishnanethi.agamotto.repositories.UserProfileRepository;
import com.srikrishnanethi.agamotto.service.NotificationService;
import com.srikrishnanethi.agamotto.service.SchedulePlanService;
import com.srikrishnanethi.agamotto.service.scheduler.GeneratedSchedule;
import com.srikrishnanethi.agamotto.service.scheduler.GreedyPlacer;
import com.srikrishnanethi.agamotto.service.scheduler.RescheduleResult;
import com.srikrishnanethi.agamotto.service.scheduler.ScheduleResult;
import com.srikrishnanethi.agamotto.service.scheduler.SchedulerEngine;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

@Service
public class SchedulePlanServiceImpl implements SchedulePlanService {
    private static final EnumSet<TaskStatus> SCHEDULABLE = EnumSet.of(
            TaskStatus.PENDING,
            TaskStatus.IN_PROGRESS);

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserProfileRepository userProfileRepository;
    private final SchedulePlanRepository schedulePlanRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final SchedulerEngine schedulerEngine;
    private final NotificationService notificationService;

    public SchedulePlanServiceImpl(ProjectRepository projectRepository,
                                   TaskRepository taskRepository,
                                   UserProfileRepository userProfileRepository,
                                   SchedulePlanRepository schedulePlanRepository,
                                   ScheduleBlockRepository scheduleBlockRepository,
                                   SchedulerEngine schedulerEngine,
                                   NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userProfileRepository = userProfileRepository;
        this.schedulePlanRepository = schedulePlanRepository;
        this.scheduleBlockRepository = scheduleBlockRepository;
        this.schedulerEngine = schedulerEngine;
        this.notificationService = notificationService;
    }


    @Override
    @Transactional
    public GeneratedSchedule generateAndPersist(String projectId, LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        String ownerId = project.getOwner().getId();
        UserProfile profile = userProfileRepository.findByUserId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "UserProfile not found for project owner: " + ownerId));

        List<Task> tasks = this.taskRepository.findByProjectIdAndStatusIn(projectId, SCHEDULABLE);
        ScheduleResult result = schedulerEngine.generate(tasks, startDate, endDate, profile);

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
        initializePlanGraph(saved);
        notifyOwner(project, saved);
        return new GeneratedSchedule(saved, result.explanationSummary());
    }

    @Override
    @Transactional(readOnly = true)
    public SchedulePlan getById(String planId) {
        SchedulePlan plan = schedulePlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("SchedulePlan not found: " + planId));
        initializePlanGraph(plan);
        return plan;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchedulePlan> listByProject(String projectId) {
        List<SchedulePlan> plans = schedulePlanRepository.findByProjectIdOrderByGeneratedAtDesc(projectId);
        plans.forEach(SchedulePlanServiceImpl::initializePlanGraph);
        return plans;
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleBlock getBlockById(String blockId) {
        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduleBlock not found: " + blockId));
        initializeBlockGraph(block);
        return block;
    }

    @Override
    @Transactional
    public ScheduleBlock overrideBlock(String blockId,
                                       LocalDateTime startTime,
                                       LocalDateTime endTime,
                                       BlockDecision decision,
                                       String reason) {
        ScheduleBlock block = getBlockById(blockId);

        if (startTime != null) {
            block.setStartTime(startTime);
        }
        if (endTime != null) {
            block.setEndTime(endTime);
        }
        if (decision != null) {
            block.setDecision(decision);
        }
        if (reason != null && !reason.isBlank()) {
            block.setReason(GreedyPlacer.clampReason(reason.trim()));
        }

        LocalDateTime start = block.getStartTime();
        LocalDateTime end = block.getEndTime();
        if (start != null && end != null && !end.isAfter(start)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        if (block.getDecision() == BlockDecision.SCHEDULED && start != null && end != null) {
            assertNoScheduledOverlap(block, start, end);
        }

        block.setManuallyOverridden(true);
        if (block.getReason() == null || block.getReason().isBlank()) {
            block.setReason(GreedyPlacer.clampReason("Manually overridden by user"));
        }
        ScheduleBlock saved = scheduleBlockRepository.save(block);
        initializeBlockGraph(saved);
        return saved;
    }

    @Override
    @Transactional
    public RescheduleResult rescheduleBlock(String blockId, LocalDateTime startTime, LocalDateTime endTime, String reason) {
        ScheduleBlock block = getBlockById(blockId);

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime are required to reschedule a block");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        assertNoScheduledOverlap(block, startTime, endTime);

        block.setStartTime(startTime);
        block.setEndTime(endTime);
        block.setDecision(BlockDecision.SCHEDULED);
        block.setManuallyOverridden(true);
        block.setReason(reason != null && !reason.isBlank()
                ? GreedyPlacer.clampReason(reason.trim())
                : "Rescheduled by user to " + startTime + " – " + endTime);
        ScheduleBlock saved = scheduleBlockRepository.save(block);
        initializeBlockGraph(saved);
        return new RescheduleResult(saved, null);
    }

    private void assertNoScheduledOverlap(ScheduleBlock moving, LocalDateTime start, LocalDateTime end) {
        String planId = moving.getSchedule().getId();
        List<ScheduleBlock> siblings = scheduleBlockRepository.findByScheduleIdOrderByStartTime(planId);
        for (ScheduleBlock other : siblings) {
            if (other.getId() != null && other.getId().equals(moving.getId())) {
                continue;
            }
            if (other.getDecision() != BlockDecision.SCHEDULED) {
                continue;
            }
            LocalDateTime otherStart = other.getStartTime();
            LocalDateTime otherEnd = other.getEndTime();
            if (otherStart == null || otherEnd == null) {
                continue;
            }
            if (start.isBefore(otherEnd) && otherStart.isBefore(end)) {
                throw new ScheduleConflictException(
                        "Schedule conflict: block overlaps another SCHEDULED block on the same plan"
                                + " (" + otherStart + " – " + otherEnd + ")");
            }
        }
    }

    private void notifyOwner(Project project, SchedulePlan plan) {
        User owner = project.getOwner();
        if (owner == null) {
            return;
        }
        String summary = plan.getExplanationSummary() == null ? "" : plan.getExplanationSummary();
        notificationService.notifyUser(
                owner,
                null,
                "Schedule generated in " + plan.getMode() + " mode. " + summary);
        for (ScheduleBlock block : plan.getBlocks()) {
            if (block.getDecision() != BlockDecision.EXCLUDED && block.getDecision() != BlockDecision.DELAYED) {
                continue;
            }
            Task task = block.getTask();
            String title = task == null || task.getTitle() == null ? "a task" : task.getTitle();
            String reason = block.getReason() == null ? block.getDecision().name() : block.getReason();
            notificationService.notifyUser(owner, task, "Task '" + title + "' was " +
                    block.getDecision().name().toLowerCase() + ": " + reason);
        }
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

    private static void initializePlanGraph(SchedulePlan plan) {
        if (plan.getProject() != null) {
            Hibernate.initialize(plan.getProject());
        }
        Hibernate.initialize(plan.getBlocks());
        for (ScheduleBlock block : plan.getBlocks()) {
            initializeBlockGraph(block);
        }
    }

    private static void initializeBlockGraph(ScheduleBlock block) {
        if (block.getSchedule() != null) {
            Hibernate.initialize(block.getSchedule());
            if (block.getSchedule().getProject() != null) {
                Hibernate.initialize(block.getSchedule().getProject());
            }
        }
        if (block.getTask() != null) {
            Hibernate.initialize(block.getTask());
        }
    }
}
