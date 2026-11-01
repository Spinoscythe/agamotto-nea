package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.SchedulePlan;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.TaskDependency;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import com.srikrishnanethi.agamotto.entities.enums.TaskStatus;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.ScheduleBlockRepository;
import com.srikrishnanethi.agamotto.repositories.SchedulePlanRepository;
import com.srikrishnanethi.agamotto.repositories.TaskDependencyRepository;
import com.srikrishnanethi.agamotto.repositories.TaskRepository;
import com.srikrishnanethi.agamotto.repositories.UserProfileRepository;
import com.srikrishnanethi.agamotto.service.scheduler.GeneratedSchedule;
import com.srikrishnanethi.agamotto.service.scheduler.RescheduleResult;
import com.srikrishnanethi.agamotto.service.scheduler.ScheduleBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

final class ScheduleBlockEditor {

    private final UserProfileRepository userProfileRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final SchedulePlanRepository schedulePlanRepository;
    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final ScheduleBuilder scheduleBuilder;
    private final ScheduleOverlapValidator scheduleOverlapValidator;
    private final SchedulePlanNotifier schedulePlanNotifier;

    ScheduleBlockEditor(UserProfileRepository userProfileRepository,
                        ScheduleBlockRepository scheduleBlockRepository,
                        SchedulePlanRepository schedulePlanRepository,
                        TaskRepository taskRepository,
                        TaskDependencyRepository taskDependencyRepository,
                        ScheduleBuilder scheduleBuilder,
                        ScheduleOverlapValidator scheduleOverlapValidator,
                        SchedulePlanNotifier schedulePlanNotifier) {
        this.userProfileRepository = userProfileRepository;
        this.scheduleBlockRepository = scheduleBlockRepository;
        this.schedulePlanRepository = schedulePlanRepository;
        this.taskRepository = taskRepository;
        this.taskDependencyRepository = taskDependencyRepository;
        this.scheduleBuilder = scheduleBuilder;
        this.scheduleOverlapValidator = scheduleOverlapValidator;
        this.schedulePlanNotifier = schedulePlanNotifier;
    }

    ScheduleBlock overrideBlock(String blockId,
                                LocalDateTime startTime,
                                LocalDateTime endTime,
                                BlockDecision decision,
                                String reason,
                                Boolean completed) {
        ScheduleBlock block = loadBlock(blockId);
        boolean wasCompleted = block.isCompleted();

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
            block.setReason(ScheduleBuilder.clampReason(reason.trim()));
        }
        if (completed != null) {
            block.setCompleted(completed);
            if (completed != wasCompleted) {
                syncTaskStatus(block, completed);
            }
        }

        LocalDateTime start = block.getStartTime();
        LocalDateTime end = block.getEndTime();
        if (start != null && end != null && !end.isAfter(start)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        if (block.getDecision() == BlockDecision.SCHEDULED && start != null && end != null) {
            scheduleOverlapValidator.assertNoScheduledOverlap(block, start, end);
        }

        block.setManuallyOverridden(true);
        if (block.getReason() == null || block.getReason().isBlank()) {
            block.setReason(ScheduleBuilder.clampReason("Manually overridden by user"));
        }
        ScheduleBlock saved = scheduleBlockRepository.save(block);
        SchedulePlanGraphInitializer.initializeBlockGraph(saved);
        return saved;
    }

    RescheduleResult rescheduleBlock(String blockId, LocalDateTime startTime, LocalDateTime endTime, String reason) {
        ScheduleBlock block = loadBlock(blockId);

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime are required to reschedule a block");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        scheduleOverlapValidator.assertNoScheduledOverlap(block, startTime, endTime);

        block.setStartTime(startTime);
        block.setEndTime(endTime);
        block.setDecision(BlockDecision.SCHEDULED);
        block.setManuallyOverridden(true);
        block.setReason(reason != null && !reason.isBlank()
                ? ScheduleBuilder.clampReason(reason.trim())
                : "Rescheduled by user to " + startTime + " – " + endTime);
        ScheduleBlock saved = scheduleBlockRepository.save(block);
        SchedulePlanGraphInitializer.initializeBlockGraph(saved);
        return new RescheduleResult(saved, null);
    }

    GeneratedSchedule markUnattendedAndRearrange(String blockId) {
        ScheduleBlock targetBlock = loadBlock(blockId);
        SchedulePlan plan = targetBlock.getSchedule();
        if (plan == null) {
            throw new ResourceNotFoundException("Schedule block is not associated with a plan: " + blockId);
        }
        Project project = plan.getProject();
        String ownerId = project.getOwner().getId();
        UserProfile profile = userProfileRepository.findByUserId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile not found for project owner: " + ownerId));

        List<ScheduleBlock> existingBlocks = scheduleBlockRepository.findByScheduleIdOrderByStartTime(plan.getId());
        if (scheduleBuilder == null) {
            throw new IllegalStateException("ScheduleBuilder is not available");
        }

        // Load dependency edges for this project so the rescheduler respects prerequisites
        List<TaskDependency> dependencyEdges = taskDependencyRepository.findByProjectId(project.getId());

        List<ScheduleBlock> rearranged = scheduleBuilder.rearrangeForUnattendedSession(
                existingBlocks, targetBlock, plan.getEndDate(), profile, dependencyEdges);

        // Delete previous blocks for this plan and set new rearranged blocks
        scheduleBlockRepository.deleteAll(existingBlocks);
        scheduleBlockRepository.flush();

        plan.getBlocks().clear();
        for (ScheduleBlock b : rearranged) {
            b.setSchedule(plan);
            plan.getBlocks().add(b);
        }
        plan.setGeneratedAt(Instant.now());

        boolean hasError = scheduleBuilder.hasDeadlineApproachedError(rearranged);
        if (hasError) {
            ScheduleBlock errorBlock = rearranged.stream()
                    .filter(b -> b.getDecision() == BlockDecision.DELAYED && b.getReason() != null && b.getReason().contains(ScheduleBuilder.DEADLINE_APPROACHED_ERROR_KEY))
                    .findFirst()
                    .orElse(null);
            String errorMsg = errorBlock != null ? errorBlock.getReason() : "Rearranging schedule after unattended session approached the day of the deadline; you can't do anything anymore.";
            plan.setExplanationSummary(errorMsg);
        } else {
            plan.setExplanationSummary("Schedule rearranged after unattended session.");
        }

        SchedulePlan saved = schedulePlanRepository.save(plan);
        SchedulePlanGraphInitializer.initializePlanGraph(saved);
        schedulePlanNotifier.notifyOwner(project, saved);

        return new GeneratedSchedule(saved, saved.getExplanationSummary());
    }

    private void syncTaskStatus(ScheduleBlock block, boolean completed) {
        Task task = block.getTask();
        if (task == null || task.getStatus() == TaskStatus.CANCELLED) {
            return;
        }
        TaskStatus status = completed ? TaskStatus.COMPLETED : TaskStatus.IN_PROGRESS;
        if (task.getStatus() == status) {
            return;
        }
        task.setStatus(status);
        task.setUpdatedAt(Instant.now());
        taskRepository.save(task);
    }

    private ScheduleBlock loadBlock(String blockId) {
        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduleBlock not found: " + blockId));
        SchedulePlanGraphInitializer.initializeBlockGraph(block);
        return block;
    }
}
