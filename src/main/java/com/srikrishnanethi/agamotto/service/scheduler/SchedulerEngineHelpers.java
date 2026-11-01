package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import com.srikrishnanethi.agamotto.entities.enums.ScheduleMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Stateless validation and block-building helpers for {@link SchedulerEngine}.
 */
final class SchedulerEngineHelpers {

    private SchedulerEngineHelpers() {
    }

    static boolean isWeekend(LocalDate day) {
        return day.getDayOfWeek().getValue() >= 6;
    }

    static double sumEffectiveHours(List<Task> tasks, ScoringStrategy scoringStrategy) {
        double sum = 0.0;
        for (Task task : tasks) {
            sum += scoringStrategy.getEffectiveDurationHoursFor(task);
        }
        return sum;
    }

    static ScheduleBlock createBlock(
            Task task,
            LocalDateTime start,
            LocalDateTime end,
            BlockDecision decision,
            String reason) {
        ScheduleBlock block = new ScheduleBlock();
        block.setTask(task);
        block.setStartTime(start);
        block.setEndTime(end);
        block.setDecision(decision);
        block.setReason(ScheduleBuilder.clampReason(reason));
        block.setManuallyOverridden(false);
        return block;
    }

    static String buildExcludedReason(
            Task task,
            double availableHours,
            double totalHours,
            ScoringStrategy scoringStrategy) {
        return "Excluded by best-fit: priority=" + task.getPriority()
                + ", duration=" + formatHours(scoringStrategy.getEffectiveDurationHoursFor(task)) + "h"
                + ", deadline urgency relative to overflow"
                + " (total=" + formatHours(totalHours) + "h > available="
                + formatHours(availableHours) + "h). Lower priority / shorter tasks removed first.";
    }

    static ScheduleResult emptyWindowResult(List<Task> tasks, UserProfile profile) {
        List<ScheduleBlock> blocks = new ArrayList<>();
        String reason = profile.worksOnWeekends()
                ? "No working days in the selected range."
                : "No working days in the selected range (weekends are excluded).";
        for (Task task : tasks) {
            blocks.add(createBlock(task, null, null, BlockDecision.EXCLUDED, reason));
        }
        String summary = reason + " All " + tasks.size() + " task(s) were left unplaced.";
        return new ScheduleResult(ScheduleMode.CRUNCH, summary, List.copyOf(blocks));
    }

    static void validateRange(LocalDate start, LocalDate end, int maxPlanDays) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end must be on or after start");
        }
        if (start.equals(LocalDate.MAX) || end.equals(LocalDate.MAX)) {
            throw new IllegalArgumentException("plan dates must be real calendar days");
        }
        long inclusiveDays = ChronoUnit.DAYS.between(start, end) + 1;
        if (inclusiveDays > maxPlanDays) {
            throw new IllegalArgumentException(
                    "plan range must be at most " + maxPlanDays + " days");
        }
    }

    static void validateProfile(UserProfile profile, ScheduleBuilder scheduleBuilder) {
        scheduleBuilder.workingHoursPerDay(profile);
        if (!Double.isFinite(profile.getWeightPriority())
                || !Double.isFinite(profile.getWeightUrgency())
                || !Double.isFinite(profile.getWeightDuration())) {
            throw new IllegalArgumentException("score weights must be finite numbers");
        }
    }

    static void validateTasks(List<Task> tasks, ScoringStrategy scoringStrategy) {
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (task == null) {
                throw new IllegalArgumentException("tasks must not contain null (index " + i + ")");
            }
            if (task.getDeadline() == null) {
                throw new IllegalArgumentException("Task '" + taskLabel(task) + "' is missing a deadline");
            }
            try {
                scoringStrategy.getEffectiveDurationHoursFor(task);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Task '" + taskLabel(task) + "' has invalid duration: " + ex.getMessage(), ex);
            }
        }
    }

    static String taskLabel(Task task) {
        if (task.getTitle() != null && !task.getTitle().isBlank()) {
            return task.getTitle();
        }
        return task.getId() != null ? task.getId() : "untitled";
    }

    static String formatHours(double hours) {
        return String.format(Locale.ROOT, "%.2f", hours);
    }
}
