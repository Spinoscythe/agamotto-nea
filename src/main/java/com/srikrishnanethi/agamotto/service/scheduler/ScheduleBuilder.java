package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.TaskDependency;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Facade for day-by-day schedule placement and unattended-session rearrangement.
 */
@Component
public class ScheduleBuilder {

    public static final int HIGH_COMPLEXITY_THRESHOLD = 4;
    public static final double EASY_SESSION_HOURS = 0.5;
    public static final double MEDIUM_SESSION_HOURS = 1.0;
    public static final double HARD_SESSION_HOURS = 2.0;
    public static final double MAX_HIGH_COMPLEXITY_SESSION_HOURS = HARD_SESSION_HOURS;
    public static final int MAX_REASON_LENGTH = 1000;
    public static final String DEADLINE_APPROACHED_ERROR_KEY = "you can't do anything anymore";

    static final double ROUNDING_TOLERANCE = 1e-9;
    static final double MINUTES_PER_HOUR = 60.0;

    private final ScheduleReasonFormatter reasonFormatter;
    private final SchedulePlacementPlanner placementPlanner;
    private final UnattendedSessionRearranger unattendedSessionRearranger;

    public ScheduleBuilder(ScoringStrategy scoringStrategy, DependencyGraphService dependencyGraph) {
        this.reasonFormatter = new ScheduleReasonFormatter(scoringStrategy);
        this.placementPlanner = new SchedulePlacementPlanner(scoringStrategy, dependencyGraph);
        this.unattendedSessionRearranger = new UnattendedSessionRearranger(scoringStrategy, placementPlanner);
    }

    public List<ScheduleBlock> buildSchedule(
            List<Task> tasks,
            LocalDate startDate,
            LocalDate endDate,
            UserProfile userProfile) {
        return buildSchedule(tasks, startDate, endDate, userProfile, null, null, List.of());
    }

    public List<ScheduleBlock> buildSchedule(
            List<Task> tasks,
            LocalDate startDate,
            LocalDate endDate,
            UserProfile userProfile,
            @Nullable LocalDateTime resumeFromTime,
            @Nullable Map<Task, Double> initialRemainingHours,
            List<TaskDependency> dependencyEdges) {
        return placementPlanner.buildSchedule(tasks, startDate, endDate, userProfile,
                resumeFromTime, initialRemainingHours, dependencyEdges);
    }

    public List<ScheduleBlock> rearrangeForUnattendedSession(
            List<ScheduleBlock> existingBlocks,
            ScheduleBlock unattendedBlock,
            LocalDate endDate,
            UserProfile userProfile,
            List<TaskDependency> dependencyEdges) {
        return unattendedSessionRearranger.rearrangeForUnattendedSession(existingBlocks, unattendedBlock, endDate, userProfile, dependencyEdges);
    }

    public double workingHoursPerDay(UserProfile userProfile) {
        return WorkingDayCalculator.workingHoursPerDay(userProfile);
    }

    public static double getMaxSessionHours(Task task) {
        return getMaxSessionHoursByComplexity(task.getComplexity());
    }

    public static double getMaxSessionHoursByComplexity(int complexity) {
        if (complexity <= 2) {
            return EASY_SESSION_HOURS;
        } else if (complexity == 3) {
            return MEDIUM_SESSION_HOURS;
        } else {
            return HARD_SESSION_HOURS;
        }
    }

    public String buildUnattendedDeadlineApproachedReason(
            Task task,
            double unplacedHours,
            LocalDate deadlineDate,
            boolean isDeadlinePastOrToday) {
        return reasonFormatter.buildUnattendedDeadlineApproachedReason(task, unplacedHours, deadlineDate, isDeadlinePastOrToday);
    }

    public boolean hasDeadlineApproachedError(List<ScheduleBlock> blocks) {
        return reasonFormatter.hasDeadlineApproachedError(blocks);
    }

    public void validateNoDeadlineApproachedError(List<ScheduleBlock> blocks) {
        reasonFormatter.validateNoDeadlineApproachedError(blocks);
    }

    public static String clampReason(String reason) {
        return ScheduleReasonFormatter.clampReason(reason);
    }
}
