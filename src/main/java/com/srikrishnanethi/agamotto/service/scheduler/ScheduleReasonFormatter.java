package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Builds the human-readable explanations stored on schedule blocks.
 */
public class ScheduleReasonFormatter {

    private final ScoringStrategy scoringStrategy;

    public ScheduleReasonFormatter(ScoringStrategy scoringStrategy) {
        this.scoringStrategy = scoringStrategy;
    }

    public String buildScheduledReason(
            Task task,
            double priorityScore,
            double sessionHours,
            LocalDate sessionDate,
            boolean needsAnotherSession,
            boolean splitDueToHighDifficulty,
            boolean decomposedAcrossDays) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("Scheduled on ").append(sessionDate)
                .append(" for ").append(formatHours(sessionHours)).append("h")
                .append(" because score=").append(formatHours(priorityScore))
                .append(" (priority=").append(task.getPriority())
                .append(", deadline=").append(task.getDeadline())
                .append(", duration=").append(formatHours(scoringStrategy.getEffectiveDurationHoursFor(task))).append("h)");
        if (splitDueToHighDifficulty) {
            explanation.append(". Session split: complexity=")
                    .append(task.getComplexity())
                    .append(" >= ").append(ScheduleBuilder.HIGH_COMPLEXITY_THRESHOLD)
                    .append(" so sessions are capped at ")
                    .append(formatHours(ScheduleBuilder.MAX_HIGH_COMPLEXITY_SESSION_HOURS)).append("h");
        } else if (decomposedAcrossDays) {
            explanation.append(". Decomposed across days to balance workload and prevent burnout");
        } else if (needsAnotherSession) {
            explanation.append(". Split across days because remaining work exceeds today's free hours");
        }
        return explanation.toString();
    }

    public String buildDelayedReason(Task task, double unplacedHours, LocalDate scheduleEndDate) {
        return "Delayed: " + formatHours(unplacedHours)
                + "h could not be placed before plan end " + scheduleEndDate
                + " (priority=" + task.getPriority()
                + ", deadline=" + task.getDeadline() + ").";
    }

    public String buildUnattendedDeadlineApproachedReason(
            Task task,
            double unplacedHours,
            LocalDate deadlineDate,
            boolean isDeadlinePastOrToday) {
        if (isDeadlinePastOrToday) {
            return clampReason("Delayed: Error: Rearranging schedule after unattended session approached the day of the deadline ("
                    + deadlineDate + "). The deadline has already arrived or passed; you can't do anything anymore.");
        }
        return clampReason("Delayed: Error: Rearranging schedule after unattended session approached the day of the deadline ("
                + deadlineDate + "). Remaining " + formatHours(unplacedHours)
                + "h cannot be accommodated before deadline — you can't do anything anymore.");
    }

    public boolean hasDeadlineApproachedError(List<ScheduleBlock> blocks) {
        if (blocks == null) return false;
        return blocks.stream()
                .filter(b -> b.getDecision() == BlockDecision.DELAYED)
                .anyMatch(b -> b.getReason() != null
                        && b.getReason().contains(ScheduleBuilder.DEADLINE_APPROACHED_ERROR_KEY));
    }

    public void validateNoDeadlineApproachedError(List<ScheduleBlock> blocks) {
        if (!hasDeadlineApproachedError(blocks)) {
            return;
        }
        ScheduleBlock failedBlock = blocks.stream()
                .filter(b -> b.getDecision() == BlockDecision.DELAYED
                        && b.getReason() != null
                        && b.getReason().contains(ScheduleBuilder.DEADLINE_APPROACHED_ERROR_KEY))
                .findFirst()
                .orElse(null);
        String msg = failedBlock != null
                ? failedBlock.getReason()
                : "Rearranging schedule after unattended session approached the day of the deadline; you can't do anything anymore.";
        throw new IllegalStateException(msg);
    }

    public String buildZeroDurationReason(Task task) {
        return "Delayed: estimated duration is 0h so there is nothing to place"
                + " (priority=" + task.getPriority()
                + ", deadline=" + task.getDeadline() + ").";
    }

    public static String formatHours(double hours) {
        return String.format(Locale.ROOT, "%.2f", hours);
    }

    public static String clampReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Unspecified scheduling decision";
        }
        if (reason.length() <= ScheduleBuilder.MAX_REASON_LENGTH) {
            return reason;
        }
        return reason.substring(0, ScheduleBuilder.MAX_REASON_LENGTH - 3) + "...";
    }
}
