package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.TaskDependency;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class UnattendedSessionRearranger {

    private static final Logger log = LoggerFactory.getLogger(UnattendedSessionRearranger.class);

    private final ScoringStrategy scoringStrategy;
    private final SchedulePlacementPlanner placementPlanner;
    private final ScheduleReasonFormatter reasonFormatter;

    UnattendedSessionRearranger(
            ScoringStrategy scoringStrategy,
            SchedulePlacementPlanner placementPlanner) {
        this.scoringStrategy = scoringStrategy;
        this.placementPlanner = placementPlanner;
        this.reasonFormatter = new ScheduleReasonFormatter(scoringStrategy);
    }

    List<ScheduleBlock> rearrangeForUnattendedSession(
            List<ScheduleBlock> existingBlocks,
            ScheduleBlock unattendedBlock,
            LocalDate endDate,
            UserProfile userProfile,
            List<TaskDependency> dependencyEdges) {

        if (existingBlocks == null || existingBlocks.isEmpty()) {
            return new ArrayList<>();
        }
        Objects.requireNonNull(unattendedBlock, "unattendedBlock");
        Objects.requireNonNull(endDate, "endDate");
        Objects.requireNonNull(userProfile, "userProfile");

        if (unattendedBlock.getStartTime() == null || unattendedBlock.getEndTime() == null) {
            throw new IllegalArgumentException("Unattended block must have valid start and end times");
        }

        LocalDateTime unattendedStart = unattendedBlock.getStartTime();
        LocalDateTime unattendedEnd = unattendedBlock.getEndTime();

        List<ScheduleBlock> resultBlocks = new ArrayList<>();
        Map<Task, Double> completedHoursPerTask = new IdentityHashMap<>();
        Set<Task> allTasks = new LinkedHashSet<>();

        for (ScheduleBlock block : existingBlocks) {
            if (block.getTask() != null) {
                allTasks.add(block.getTask());
            }
            if (block.getDecision() != BlockDecision.SCHEDULED) {
                continue;
            }
            if (isSameBlock(block, unattendedBlock)) {
                continue;
            }

            if (block.getEndTime() != null && !block.getEndTime().isAfter(unattendedStart)) {
                resultBlocks.add(block);
                double hours = ChronoUnit.MINUTES.between(block.getStartTime(), block.getEndTime()) / ScheduleBuilder.MINUTES_PER_HOUR;
                completedHoursPerTask.merge(block.getTask(), hours, Double::sum);
            }
        }

        double missedHours = ChronoUnit.MINUTES.between(unattendedStart, unattendedEnd) / ScheduleBuilder.MINUTES_PER_HOUR;
        ScheduleBlock missedRecord = SchedulerEngineHelpers.createBlock(
                unattendedBlock.getTask(),
                unattendedStart,
                unattendedEnd,
                BlockDecision.SCHEDULED,
                ScheduleReasonFormatter.clampReason("Unattended session (" + ScheduleReasonFormatter.formatHours(missedHours)
                        + "h missed). Schedule rearranged to accommodate uncompleted work."));
        missedRecord.setManuallyOverridden(true);
        resultBlocks.add(missedRecord);

        Map<Task, Double> remainingHoursPerTask = new IdentityHashMap<>();
        List<Task> pendingTasks = new ArrayList<>();
        for (Task task : allTasks) {
            double totalRequired = scoringStrategy.getEffectiveDurationHoursFor(task);
            double completed = completedHoursPerTask.getOrDefault(task, 0.0);
            double remaining = Math.max(0.0, totalRequired - completed);
            if (remaining > ScheduleBuilder.ROUNDING_TOLERANCE) {
                remainingHoursPerTask.put(task, remaining);
                pendingTasks.add(task);
            }
        }

        LocalDate resumeDate = unattendedEnd.toLocalDate();
        Map<Task, Double> postPlacedHoursPerTask = new IdentityHashMap<>();

        if (!resumeDate.isAfter(endDate) && !pendingTasks.isEmpty()) {
            List<ScheduleBlock> rawRearrangedBlocks = placementPlanner.buildSchedule(
                    pendingTasks,
                    resumeDate,
                    endDate,
                    userProfile,
                    unattendedEnd,
                    remainingHoursPerTask,
                    dependencyEdges);

            List<ScheduleBlock> validBlocks = new ArrayList<>();

            for (ScheduleBlock block : rawRearrangedBlocks) {
                if (block.getDecision() != BlockDecision.SCHEDULED || block.getTask() == null) {
                    continue;
                }
                Task task = block.getTask();
                LocalDateTime deadline = task.getDeadline();
                if (deadline == null) {
                    validBlocks.add(block);
                    double h = ChronoUnit.MINUTES.between(block.getStartTime(), block.getEndTime()) / ScheduleBuilder.MINUTES_PER_HOUR;
                    postPlacedHoursPerTask.merge(task, h, Double::sum);
                    continue;
                }

                if (!block.getStartTime().isBefore(deadline)) {
                    continue;
                }

                if (block.getEndTime().isAfter(deadline)) {
                    long validMinutes = ChronoUnit.MINUTES.between(block.getStartTime(), deadline);
                    if (validMinutes > 0) {
                        double validHours = validMinutes / ScheduleBuilder.MINUTES_PER_HOUR;
                        ScheduleBlock truncated = SchedulerEngineHelpers.createBlock(
                                task,
                                block.getStartTime(),
                                deadline,
                                BlockDecision.SCHEDULED,
                                block.getReason());
                        validBlocks.add(truncated);
                        postPlacedHoursPerTask.merge(task, validHours, Double::sum);
                    }
                } else {
                    validBlocks.add(block);
                    double h = ChronoUnit.MINUTES.between(block.getStartTime(), block.getEndTime()) / ScheduleBuilder.MINUTES_PER_HOUR;
                    postPlacedHoursPerTask.merge(task, h, Double::sum);
                }
            }

            resultBlocks.addAll(validBlocks);
        }

        for (Task task : pendingTasks) {
            double required = remainingHoursPerTask.getOrDefault(task, 0.0);
            double placed = postPlacedHoursPerTask.getOrDefault(task, 0.0);
            double unplaced = Math.max(0.0, required - placed);
            if (unplaced > ScheduleBuilder.ROUNDING_TOLERANCE) {
                String errorReason;
                if (task.getDeadline() != null) {
                    LocalDate dl = task.getDeadline().toLocalDate();
                    boolean pastOrToday = !unattendedEnd.isBefore(task.getDeadline()) || !resumeDate.isBefore(dl);
                    errorReason = reasonFormatter.buildUnattendedDeadlineApproachedReason(task, unplaced, dl, pastOrToday);
                    log.error("Rearranging schedule after unattended session approached the day of the deadline ({}) for task '{}': you can't do anything anymore.",
                            dl, task.getTitle());
                } else {
                    errorReason = reasonFormatter.buildDelayedReason(task, unplaced, endDate);
                }
                resultBlocks.add(SchedulerEngineHelpers.createBlock(task, null, null, BlockDecision.DELAYED, errorReason));
            }
        }

        return resultBlocks;
    }

    private boolean isSameBlock(ScheduleBlock a, ScheduleBlock b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.getId() != null && b.getId() != null) {
            return a.getId().equals(b.getId());
        }
        return Objects.equals(a.getStartTime(), b.getStartTime())
                && Objects.equals(a.getEndTime(), b.getEndTime())
                && Objects.equals(a.getTask(), b.getTask());
    }
}
