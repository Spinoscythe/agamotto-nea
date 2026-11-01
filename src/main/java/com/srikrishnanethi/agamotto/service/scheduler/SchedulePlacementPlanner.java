package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.TaskDependency;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Day-by-day greedy placement used by Serenity and by Crunch survivors.
 *
 * <h2>Why these data structures</h2>
 * <ul>
 *   <li>{@link PriorityQueue} (binary heap) — the ready set is not static. A task can be
 *       only partly placed, {@code offer}ed back, and must then compete against whatever is
 *       still unfinished. Heap {@code poll}/{@code offer} are O(log n). A one-off
 *       {@code ArrayList.sort} would be wrong because the set of remaining tasks changes
 *       after every chunk. A {@code TreeSet} was rejected: equal scores are allowed, and
 *       the same task object is removed and re-inserted after its remaining hours change.</li>
 *   <li>{@link IdentityHashMap} for remaining hours — {@code Task.equals}/{@code hashCode}
 *       are id-based ({@code BaseEntity}). Unsaved tasks (and some Hibernate proxies) can
 *       share a null id, so a normal {@code HashMap} would collide keys. Identity hashing
 *       keys by reference so each queue entry keeps its own leftover hours.</li>
 *   <li>{@link ArrayList} for the output blocks — we only append, in the order the calendar
 *       is filled. The week UI walks that sequence; a linked list would add pointer overhead
 *       for no benefit at NEA-sized n (tens of tasks, not millions).</li>
 * </ul>
 */
final class SchedulePlacementPlanner {

    private final ScoringStrategy scoringStrategy;
    private final ScheduleReasonFormatter reasonFormatter;
    private final DependencyGraphService dependencyGraph;

    SchedulePlacementPlanner(ScoringStrategy scoringStrategy, DependencyGraphService dependencyGraph) {
        this.scoringStrategy = scoringStrategy;
        this.reasonFormatter = new ScheduleReasonFormatter(scoringStrategy);
        this.dependencyGraph = dependencyGraph;
    }

    List<ScheduleBlock> buildSchedule(
            List<Task> tasks,
            LocalDate startDate,
            LocalDate endDate,
            UserProfile userProfile,
            @Nullable LocalDateTime resumeFromTime,
            @Nullable Map<Task, Double> initialRemainingHours,
            List<TaskDependency> dependencyEdges) {
        List<ScheduleBlock> scheduledBlocks = new ArrayList<>();
        if (tasks == null || tasks.isEmpty()) {
            return scheduledBlocks;
        }
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        Objects.requireNonNull(userProfile, "userProfile");
        Map<Task, Double> remainingHoursPerTask = new IdentityHashMap<>();
        if (initialRemainingHours != null) {
            remainingHoursPerTask.putAll(initialRemainingHours);
        } else {
            tasks.forEach(task -> remainingHoursPerTask.put(task, scoringStrategy.getEffectiveDurationHoursFor(task)));
        }
        LocalTime workDayStartTime = userProfile.getPreferredStart();
        LocalTime workDayEndTime = userProfile.getPreferredEnd();
        double dailyWorkingHours = WorkingDayCalculator.workingHoursPerDay(userProfile);

        // Tasks whose prerequisites are fully placed (completed) — ready to be scheduled.
        // A task is "completed" in the planning sense when all its hours have been placed.
        Set<String> placedTaskIds = new HashSet<>();

        // Start with tasks that have no prerequisites — they're immediately ready.
        Set<Task> tasksForTomorrow = new LinkedHashSet<>();
        for (Task task : tasks) {
            if (dependencyGraph.arePrerequisitesMet(task, placedTaskIds, dependencyEdges)) {
                tasksForTomorrow.add(task);
            }
        }

        LocalDate currentDay = startDate;
        while (!currentDay.isAfter(endDate) && !tasksForTomorrow.isEmpty()) {
            if (!userProfile.worksOnWeekends() && WorkingDayCalculator.isWeekend(currentDay)) {
                currentDay = WorkingDayCalculator.nextDayOrNull(currentDay);
                if (currentDay == null) {
                    break;
                }
                continue;
            }

            Set<Task> tasksForToday = new LinkedHashSet<>(tasksForTomorrow);
            tasksForTomorrow.clear();

            PriorityQueue<Task> tasksToSchedule = new PriorityQueue<>(taskPlacementComparator(userProfile, currentDay));
            for (Task t : tasksForToday) {
                if (remainingHoursPerTask.getOrDefault(t, 0.0) > ScheduleBuilder.ROUNDING_TOLERANCE) {
                    tasksToSchedule.add(t);
                }
            }

            List<Task> tasksForTodayNextRound = new ArrayList<>();
            Map<Task, Double> hoursPlacedToday = new IdentityHashMap<>();

            double availableHoursToday = dailyWorkingHours;
            LocalDateTime currentWorkTime = LocalDateTime.of(currentDay, workDayStartTime);
            LocalDateTime endOfWorkDay = LocalDateTime.of(currentDay, workDayEndTime);

            if (resumeFromTime != null && currentDay.equals(resumeFromTime.toLocalDate())) {
                if (resumeFromTime.toLocalTime().isAfter(workDayStartTime)) {
                    currentWorkTime = LocalDateTime.of(currentDay, resumeFromTime.toLocalTime());
                }
                if (!currentWorkTime.isBefore(endOfWorkDay)) {
                    tasksForTomorrow.addAll(tasksForToday);
                    currentDay = WorkingDayCalculator.nextDayOrNull(currentDay);
                    if (currentDay == null) {
                        break;
                    }
                    continue;
                }
                availableHoursToday = ChronoUnit.MINUTES.between(currentWorkTime.toLocalTime(), workDayEndTime) / ScheduleBuilder.MINUTES_PER_HOUR;
            }

            while (availableHoursToday > ScheduleBuilder.ROUNDING_TOLERANCE
                    && (!tasksToSchedule.isEmpty() || !tasksForTodayNextRound.isEmpty())) {
                if (tasksToSchedule.isEmpty()) {
                    tasksToSchedule.addAll(tasksForTodayNextRound);
                    tasksForTodayNextRound.clear();
                }

                if (!currentWorkTime.isBefore(endOfWorkDay)) {
                    break;
                }

                Task task = tasksToSchedule.poll();
                double remainingTaskHours = remainingHoursPerTask.getOrDefault(task, 0.0);
                if (remainingTaskHours <= ScheduleBuilder.ROUNDING_TOLERANCE) {
                    continue;
                }

                long daysRemaining = WorkingDayCalculator.countWorkingDaysUntilDeadline(currentDay, task, endDate, userProfile);
                double maxSittingHours = ScheduleBuilder.getMaxSessionHours(task);
                double alreadyPlacedToday = hoursPlacedToday.getOrDefault(task, 0.0);

                double dailyQuota;
                if (daysRemaining <= 1) {
                    dailyQuota = remainingTaskHours;
                } else {
                    double dailyPace = remainingTaskHours / daysRemaining;
                    dailyQuota = Math.max(0.5, Math.ceil(dailyPace * 2.0) / 2.0);
                }

                double quotaLeftToday = Math.max(0.0, dailyQuota - alreadyPlacedToday);
                if (daysRemaining > 1 && quotaLeftToday <= ScheduleBuilder.ROUNDING_TOLERANCE) {
                    tasksForTomorrow.add(task);
                    continue;
                }

                double targetSessionHours = Math.min(remainingTaskHours, maxSittingHours);
                if (daysRemaining > 1) {
                    targetSessionHours = Math.min(targetSessionHours, quotaLeftToday);
                }

                double sessionHours = Math.min(availableHoursToday, targetSessionHours);
                long sessionMinutes = Math.round(sessionHours * ScheduleBuilder.MINUTES_PER_HOUR);
                if (sessionMinutes <= 0) {
                    if (remainingTaskHours * ScheduleBuilder.MINUTES_PER_HOUR >= 1.0 - ScheduleBuilder.ROUNDING_TOLERANCE
                            && availableHoursToday * ScheduleBuilder.MINUTES_PER_HOUR < 1.0) {
                        tasksForTomorrow.add(task);
                        break;
                    }
                    tasksForTomorrow.add(task);
                    continue;
                }

                LocalDateTime sessionEndTime = currentWorkTime.plusMinutes(sessionMinutes);

                if (sessionEndTime.isAfter(endOfWorkDay)) {
                    sessionEndTime = endOfWorkDay;
                    sessionMinutes = ChronoUnit.MINUTES.between(currentWorkTime, sessionEndTime);
                    if (sessionMinutes <= 0) {
                        tasksForTomorrow.add(task);
                        break;
                    }
                }
                sessionHours = sessionMinutes / ScheduleBuilder.MINUTES_PER_HOUR;

                double taskPriorityScore = scoringStrategy.scoreTask(task, userProfile, currentDay);
                boolean needsAnotherSession = remainingTaskHours > sessionHours + ScheduleBuilder.ROUNDING_TOLERANCE;
                boolean splitDueToHighDifficulty = needsAnotherSession
                        && task.getComplexity() >= ScheduleBuilder.HIGH_COMPLEXITY_THRESHOLD
                        && sessionHours >= maxSittingHours - ScheduleBuilder.ROUNDING_TOLERANCE;
                boolean decomposedAcrossDays = needsAnotherSession && daysRemaining > 1;

                scheduledBlocks.add(SchedulerEngineHelpers.createBlock(
                        task,
                        currentWorkTime,
                        sessionEndTime,
                        BlockDecision.SCHEDULED,
                        reasonFormatter.buildScheduledReason(task,
                                taskPriorityScore,
                                sessionHours,
                                currentDay,
                                needsAnotherSession,
                                splitDueToHighDifficulty,
                                decomposedAcrossDays)));

                remainingTaskHours -= sessionHours;
                availableHoursToday -= sessionHours;
                currentWorkTime = sessionEndTime;
                remainingHoursPerTask.put(task, remainingTaskHours);
                hoursPlacedToday.put(task, alreadyPlacedToday + sessionHours);

                if (remainingTaskHours > ScheduleBuilder.ROUNDING_TOLERANCE) {
                    double newPlacedToday = alreadyPlacedToday + sessionHours;
                    if (daysRemaining > 1 && newPlacedToday >= dailyQuota - ScheduleBuilder.ROUNDING_TOLERANCE) {
                        tasksForTomorrow.add(task);
                    } else {
                        tasksForTodayNextRound.add(task);
                    }
                } else {
                    // Task fully placed — mark it as "completed" so its dependents can unlock.
                    placedTaskIds.add(task.getId());
                    // Check if any waiting tasks now have their prerequisites met.
                    for (Task waiting : tasks) {
                        if (!placedTaskIds.contains(waiting.getId())
                                && !tasksForTomorrow.contains(waiting)
                                && !tasksForToday.contains(waiting)
                                && !tasksToSchedule.contains(waiting)
                                && !tasksForTodayNextRound.contains(waiting)
                                && dependencyGraph.arePrerequisitesMet(waiting, placedTaskIds, dependencyEdges)) {
                            tasksForTomorrow.add(waiting);
                        }
                    }
                }
            }

            tasksForTomorrow.addAll(tasksToSchedule);
            tasksForTomorrow.addAll(tasksForTodayNextRound);
            tasksToSchedule.clear();
            tasksForTodayNextRound.clear();

            currentDay = WorkingDayCalculator.nextDayOrNull(currentDay);
            if (currentDay == null) {
                break;
            }
        }

        // Any task that still has unplaced hours becomes DELAYED.
        for (Task task : tasks) {
            double totalRequiredHours = scoringStrategy.getEffectiveDurationHoursFor(task);
            double unplacedHours = remainingHoursPerTask.getOrDefault(task, 0.0);
            if (unplacedHours > ScheduleBuilder.ROUNDING_TOLERANCE) {
                scheduledBlocks.add(SchedulerEngineHelpers.createBlock(
                        task,
                        null,
                        null,
                        BlockDecision.DELAYED,
                        reasonFormatter.buildDelayedReason(task, unplacedHours, endDate)));
            } else if (totalRequiredHours <= ScheduleBuilder.ROUNDING_TOLERANCE) {
                scheduledBlocks.add(SchedulerEngineHelpers.createBlock(
                        task,
                        null,
                        null,
                        BlockDecision.DELAYED,
                        reasonFormatter.buildZeroDurationReason(task)));
            }
        }
        return scheduledBlocks;
    }

    private Comparator<Task> taskPlacementComparator(UserProfile userProfile, LocalDate asOfDate) {
        return Comparator
                .comparingDouble((Task t) -> scoringStrategy.scoreTask(t, userProfile, asOfDate)).reversed()
                .thenComparing(Task::getDeadline)
                .thenComparing(t -> t.getId() == null ? "" : t.getId());
    }
}
