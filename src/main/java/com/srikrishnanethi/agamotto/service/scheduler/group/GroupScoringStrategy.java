package com.srikrishnanethi.agamotto.service.scheduler.group;

import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.service.scheduler.ScoringStrategy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Calculates priority scores for tasks in a group project.
 *
 * <p>In addition to task priority and deadline urgency, it factors in team members'
 * current workloads so work is distributed fairly across the team.</p>
 */
@Component
public class GroupScoringStrategy {

    public static final double MAX_DEADLINE_URGENCY = 2.0;

    private final ScoringStrategy singleUserScoringStrategy;

    public GroupScoringStrategy(ScoringStrategy singleUserScoringStrategy) {
        this.singleUserScoringStrategy = singleUserScoringStrategy;
    }

    /**
     * Returns the task duration in hours (using corrected hours if available, otherwise estimated hours).
     * Required by GroupScheduleBuilder.
     */
    public double getEffectiveDurationHours(Task task) {
        return singleUserScoringStrategy.getEffectiveDurationHours(task);
    }

    /**
     * Measures how close a task is to its deadline.
     * Overdue or due-today tasks receive the highest urgency score.
     */
    public double deadlineUrgency(Task task, LocalDate asOfDate) {
        return singleUserScoringStrategy.deadlineUrgency(task, asOfDate);
    }

    /**
     * Calculates the baseline priority score for a task using the member's profile weights.
     */
    public double scoreTask(Task task, UserProfile memberProfile, LocalDate asOfDate) {
        return singleUserScoringStrategy.scoreTask(task, memberProfile, asOfDate);
    }

    /**
     * Scores how suitable a specific member is for a task based on:
     * 1. Task priority and deadline urgency.
     * 2. Member's current workload (members with lower loadRatio get higher priority).
     *
     * @param task the task to be scheduled
     * @param memberProfile the user's working hours and scoring weights
     * @param memberLoadRatio scheduledHours / totalWindowCapacity (0.0 = free, 1.0 = fully booked)
     * @param asOfDate the scheduling date
     */
    public double scoreTaskForMember(
            Task task,
            UserProfile memberProfile,
            double memberLoadRatio,
            LocalDate asOfDate) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(memberProfile, "memberProfile");
        Objects.requireNonNull(asOfDate, "asOfDate");

        double baseScore = scoreTask(task, memberProfile, asOfDate);

        // Fair-share penalty: subtract a penalty if the member is already heavily loaded.
        // A member who is 80% booked (0.80) receives a lower assignment score than someone at 20% (0.20).
        double workloadPenalty = memberLoadRatio * 5.0;

        return baseScore - workloadPenalty;
    }
}
