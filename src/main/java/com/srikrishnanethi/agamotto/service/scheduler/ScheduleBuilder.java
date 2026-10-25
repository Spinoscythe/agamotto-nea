package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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
 *   <li>{@link StringBuilder} for reason text — many fragments are appended per block;
 *       concatenating with {@code +} would copy the growing string each time.</li>
 * </ul>
 */
@Component
public class ScheduleBuilder {
    private static final Logger log = LoggerFactory.getLogger(ScheduleBuilder.class);

    public static final int HIGH_COMPLEXITY_THRESHOLD = 4;
    public static final double EASY_SESSION_HOURS = 0.5;
    public static final double MEDIUM_SESSION_HOURS = 1.0;
    public static final double HARD_SESSION_HOURS = 2.0;
    /**
     * Difficult tasks (complexity 4 or 5) are capped at 2 hours per session to prevent fatigue.
     */
    public static final double MAX_HIGH_COMPLEXITY_SESSION_HOURS = HARD_SESSION_HOURS;
    public static final int MAX_REASON_LENGTH = 1000;
    public static final String DEADLINE_APPROACHED_ERROR_KEY = "you can't do anything anymore";
    /**
     * A tiny fraction of an hour used to safely handle small rounding differences.
     */
    private static final double ROUNDING_TOLERANCE = 1e-9;
    private static final double MINUTES_PER_HOUR = 60.0;
    private final ScoringStrategy scoringStrategy;

    public ScheduleBuilder(ScoringStrategy scoringStrategy) {
        this.scoringStrategy = scoringStrategy;
    }

    public List<ScheduleBlock> buildSchedule(
            List<Task> tasks,
            LocalDate startDate,
            LocalDate endDate,
            UserProfile userProfile) {
        return buildSchedule(tasks, startDate, endDate, userProfile, null, null);
    }

    public List<ScheduleBlock> buildSchedule(
            List<Task> tasks,
            LocalDate startDate,
            LocalDate endDate,
            UserProfile userProfile,
            @Nullable LocalDateTime resumeFromTime,
            @Nullable Map<Task, Double> initialRemainingHours) {
        List<ScheduleBlock> scheduledBlocks = new ArrayList<>();
        if (tasks == null || tasks.isEmpty()) {
            return scheduledBlocks;
        }
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        Objects.requireNonNull(userProfile, "userProfile");
        // Track how many hours are left to complete for each task.
        Map<Task, Double> remainingHoursPerTask = new IdentityHashMap<>();
        if (initialRemainingHours != null) {
            remainingHoursPerTask.putAll(initialRemainingHours);
        } else {
            tasks.forEach(task -> remainingHoursPerTask.put(task, scoringStrategy.getEffectiveDurationHours(task)));
        }
        // The user's preferred working hours for each day (e.g. 09:00 to 17:00).
        LocalTime workDayStartTime = userProfile.getPreferredStart();
        LocalTime workDayEndTime = userProfile.getPreferredEnd();
        double dailyWorkingHours = workingHoursPerDay(userProfile);
        // We track tasks across days using tasksForTomorrow
        Set<Task> tasksForTomorrow = new LinkedHashSet<>(tasks);

        // Walk through the calendar day-by-day from the start date to the end date.
        LocalDate currentDay = startDate;
        while (!currentDay.isAfter(endDate) && !tasksForTomorrow.isEmpty()) {
            // Skip weekends if the user chose not to work on weekends.
            if (!userProfile.isIncludeWeekends() && isWeekend(currentDay)) {
                currentDay = nextDayOrNull(currentDay);
                if (currentDay == null) {
                    break;
                }
                continue;
            }

            // Start of day: query tasksForTomorrow to obtain today's workload
            Set<Task> tasksForToday = new LinkedHashSet<>(tasksForTomorrow);
            tasksForTomorrow.clear();

            PriorityQueue<Task> tasksToSchedule = new PriorityQueue<>(taskPlacementComparator(userProfile, startDate));
            for (Task t : tasksForToday) {
                if (remainingHoursPerTask.getOrDefault(t, 0.0) > ROUNDING_TOLERANCE) {
                    tasksToSchedule.add(t);
                }
            }

            List<Task> tasksForTodayNextRound = new ArrayList<>();
            Map<Task, Double> hoursPlacedToday = new IdentityHashMap<>();

            // Start each new day at the morning start time with a fresh budget of hours.
            double availableHoursToday = dailyWorkingHours;
            LocalDateTime currentWorkTime = LocalDateTime.of(currentDay, workDayStartTime);
            LocalDateTime endOfWorkDay = LocalDateTime.of(currentDay, workDayEndTime);

            // If resuming mid-day (e.g. after an unattended session), adjust clock and free hours
            if (resumeFromTime != null && currentDay.equals(resumeFromTime.toLocalDate())) {
                if (resumeFromTime.toLocalTime().isAfter(workDayStartTime)) {
                    currentWorkTime = LocalDateTime.of(currentDay, resumeFromTime.toLocalTime());
                }
                if (!currentWorkTime.isBefore(endOfWorkDay)) {
                    tasksForTomorrow.addAll(tasksForToday);
                    currentDay = nextDayOrNull(currentDay);
                    if (currentDay == null) {
                        break;
                    }
                    continue;
                }
                availableHoursToday = ChronoUnit.MINUTES.between(currentWorkTime.toLocalTime(), workDayEndTime) / MINUTES_PER_HOUR;
            }

            // Place tasks into available calendar slots today
            while (availableHoursToday > ROUNDING_TOLERANCE && (!tasksToSchedule.isEmpty() || !tasksForTodayNextRound.isEmpty())) {
                // Query tasksForTodayNextRound when current round finishes
                if (tasksToSchedule.isEmpty()) {
                    tasksToSchedule.addAll(tasksForTodayNextRound);
                    tasksForTodayNextRound.clear();
                }

                // If our clock has reached the end of the working day, push remaining tasks to tomorrow.
                if (!currentWorkTime.isBefore(endOfWorkDay)) {
                    break;
                }

                // Pick the task that currently has the highest priority score.
                Task task = tasksToSchedule.poll();
                double remainingTaskHours = remainingHoursPerTask.getOrDefault(task, 0.0);
                if (remainingTaskHours <= ROUNDING_TOLERANCE) {
                    continue;
                }

                // --- COMPUTATIONAL THINKING: DECOMPOSITION & ANTI-BURNOUT PACING ---
                long daysRemaining = countWorkingDaysUntilDeadline(currentDay, task, endDate, userProfile);
                double maxSittingHours = getMaxSessionHours(task);
                double alreadyPlacedToday = hoursPlacedToday.getOrDefault(task, 0.0);

                // Sensibly split workload across the days remaining until deadline:
                // E.g. A 10-hour maths block with 5 days remaining gets paced at 2.0h/day.
                double dailyQuota;
                if (daysRemaining <= 1) {
                    // Deadline is today (or overdue): schedule as much as possible up to sitting limit
                    dailyQuota = remainingTaskHours;
                } else {
                    double dailyPace = remainingTaskHours / daysRemaining;
                    dailyQuota = Math.max(0.5, Math.ceil(dailyPace * 2.0) / 2.0);
                }

                double quotaLeftToday = Math.max(0.0, dailyQuota - alreadyPlacedToday);
                if (daysRemaining > 1 && quotaLeftToday <= ROUNDING_TOLERANCE) {
                    // Today's quota reached; defer remainder to tomorrow
                    tasksForTomorrow.add(task);
                    continue;
                }

                double targetSessionHours = Math.min(remainingTaskHours, maxSittingHours);
                if (daysRemaining > 1) {
                    targetSessionHours = Math.min(targetSessionHours, quotaLeftToday);
                }

                // Calculate how long this session should be (bounded by today's available workday hours)
                double sessionHours = Math.min(availableHoursToday, targetSessionHours);
                long sessionMinutes = Math.round(sessionHours * MINUTES_PER_HOUR);
                if (sessionMinutes <= 0) {
                    if (remainingTaskHours * MINUTES_PER_HOUR >= 1.0 - ROUNDING_TOLERANCE && availableHoursToday * MINUTES_PER_HOUR < 1.0) {
                        // Less than one minute left in the day; try again tomorrow.
                        tasksForTomorrow.add(task);
                        break;
                    }
                    tasksForTomorrow.add(task);
                    continue;
                }

                LocalDateTime sessionEndTime = currentWorkTime.plusMinutes(sessionMinutes);
                // Ensure this session does not accidentally run past the end of the working day.
                if (sessionEndTime.isAfter(endOfWorkDay)) {
                    sessionEndTime = endOfWorkDay;
                    sessionMinutes = ChronoUnit.MINUTES.between(currentWorkTime, sessionEndTime);
                    if (sessionMinutes <= 0) {
                        tasksForTomorrow.add(task);
                        break;
                    }
                }
                sessionHours = sessionMinutes / MINUTES_PER_HOUR;

                // Record this scheduled block on the timetable.
                double taskPriorityScore = scoringStrategy.scoreTask(task, userProfile, startDate);
                boolean needsAnotherSession = remainingTaskHours > sessionHours + ROUNDING_TOLERANCE;
                boolean splitDueToHighDifficulty = needsAnotherSession && task.getComplexity() >= HIGH_COMPLEXITY_THRESHOLD && sessionHours >= maxSittingHours - ROUNDING_TOLERANCE;
                boolean decomposedAcrossDays = needsAnotherSession && daysRemaining > 1;
                scheduledBlocks.add(createBlock(
                        task,
                        currentWorkTime,
                        sessionEndTime,
                        BlockDecision.SCHEDULED,
                        buildScheduledReason(task,
                                taskPriorityScore,
                                sessionHours,
                                currentDay,
                                needsAnotherSession,
                                splitDueToHighDifficulty,
                                decomposedAcrossDays)));

                // Deduct the booked time from the task and from today's available hours.
                remainingTaskHours -= sessionHours;
                availableHoursToday -= sessionHours;
                currentWorkTime = sessionEndTime; // Advance the clock to the end of this session.
                remainingHoursPerTask.put(task, remainingTaskHours);
                hoursPlacedToday.put(task, alreadyPlacedToday + sessionHours);

                // Re-queue remaining work: into today's next round if quota remains, or tomorrow if quota is reached
                if (remainingTaskHours > ROUNDING_TOLERANCE) {
                    double newPlacedToday = alreadyPlacedToday + sessionHours;
                    if (daysRemaining > 1 && newPlacedToday >= dailyQuota - ROUNDING_TOLERANCE) {
                        tasksForTomorrow.add(task);
                    } else {
                        tasksForTodayNextRound.add(task);
                    }
                }
            }

            // Carry over any remaining unplaced tasks from today's queues into tomorrow
            tasksForTomorrow.addAll(tasksToSchedule);
            tasksForTomorrow.addAll(tasksForTodayNextRound);
            tasksToSchedule.clear();
            tasksForTodayNextRound.clear();

            currentDay = nextDayOrNull(currentDay);
            if (currentDay == null) {
                break;
            }
        }
        // Any tasks that could not fit before the end date are marked as DELAYED.
        for (Task task : tasks) {
            double totalRequiredHours = scoringStrategy.getEffectiveDurationHours(task);
            double unplacedHours = remainingHoursPerTask.getOrDefault(task, 0.0);
            if (unplacedHours > ROUNDING_TOLERANCE) {
                scheduledBlocks.add(createBlock(task, null, null, BlockDecision.DELAYED, buildDelayedReason(task, unplacedHours, endDate)));
            } else if (totalRequiredHours <= ROUNDING_TOLERANCE) {
                scheduledBlocks.add(createBlock(task, null, null, BlockDecision.DELAYED, buildZeroDurationReason(task)));
            }
        }
        return scheduledBlocks;
    }

    /**
     * Rearranges an existing schedule when a session is unattended by the user.
     *
     * <p>Key behaviours:
     * <ul>
     *   <li>Sessions completed prior to the unattended session remain untouched.</li>
     *   <li>The unattended block itself is retained for timetable history (marked as manually overridden
     *       with an explanatory reason), but its booked hours are NOT deducted from the task backlog.</li>
     *   <li>The missed hours and all remaining incomplete tasks are dynamically rescheduled into
     *       subsequent available calendar slots starting from the end of the unattended session.</li>
     * </ul>
     *
     * @param existingBlocks  the current plan's schedule blocks
     * @param unattendedBlock the block that was missed / unattended
     * @param endDate         the final date of the scheduling window
     * @param userProfile     the user's availability and working hour preferences
     * @return the rearranged schedule blocks
     */
    public List<ScheduleBlock> rearrangeForUnattendedSession(
            List<ScheduleBlock> existingBlocks,
            ScheduleBlock unattendedBlock,
            LocalDate endDate,
            UserProfile userProfile) {

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

        // 1. Separate past completed sessions from future sessions
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

            // Preserved blocks: finished before the unattended session started
            if (block.getEndTime() != null && !block.getEndTime().isAfter(unattendedStart)) {
                resultBlocks.add(block);
                double hours = ChronoUnit.MINUTES.between(block.getStartTime(), block.getEndTime()) / MINUTES_PER_HOUR;
                completedHoursPerTask.merge(block.getTask(), hours, Double::sum);
            }
        }

        // 2. Retain the unattended block as an audit record
        double missedHours = ChronoUnit.MINUTES.between(unattendedStart, unattendedEnd) / MINUTES_PER_HOUR;
        ScheduleBlock missedRecord = createBlock(
                unattendedBlock.getTask(),
                unattendedStart,
                unattendedEnd,
                BlockDecision.SCHEDULED,
                clampReason("Unattended session (" + formatHours(missedHours)
                        + "h missed). Schedule rearranged to accommodate uncompleted work."));
        missedRecord.setManuallyOverridden(true);
        resultBlocks.add(missedRecord);

        // 3. Recompute remaining task hours (missed session hours remain uncompleted)
        Map<Task, Double> remainingHoursPerTask = new IdentityHashMap<>();
        List<Task> pendingTasks = new ArrayList<>();
        for (Task task : allTasks) {
            double totalRequired = scoringStrategy.getEffectiveDurationHours(task);
            double completed = completedHoursPerTask.getOrDefault(task, 0.0);
            double remaining = Math.max(0.0, totalRequired - completed);
            if (remaining > ROUNDING_TOLERANCE) {
                remainingHoursPerTask.put(task, remaining);
                pendingTasks.add(task);
            }
        }

        // 4. Rearrange remaining work starting from the end of the missed session
        LocalDate resumeDate = unattendedEnd.toLocalDate();
        Map<Task, Double> postPlacedHoursPerTask = new IdentityHashMap<>();

        if (!resumeDate.isAfter(endDate) && !pendingTasks.isEmpty()) {
            List<ScheduleBlock> rawRearrangedBlocks = buildSchedule(
                    pendingTasks,
                    resumeDate,
                    endDate,
                    userProfile,
                    unattendedEnd,
                    remainingHoursPerTask);

            // Post-process rearranged blocks to strictly enforce task deadlines
            List<ScheduleBlock> validBlocks = new ArrayList<>();

            for (ScheduleBlock block : rawRearrangedBlocks) {
                if (block.getDecision() != BlockDecision.SCHEDULED || block.getTask() == null) {
                    continue; // Existing delayed blocks will be consolidated below
                }
                Task task = block.getTask();
                LocalDateTime deadline = task.getDeadline();
                if (deadline == null) {
                    validBlocks.add(block);
                    double h = ChronoUnit.MINUTES.between(block.getStartTime(), block.getEndTime()) / MINUTES_PER_HOUR;
                    postPlacedHoursPerTask.merge(task, h, Double::sum);
                    continue;
                }

                // If block starts at or after the deadline, it cannot be scheduled
                if (!block.getStartTime().isBefore(deadline)) {
                    continue;
                }

                // If block runs past the deadline, clamp it to the deadline
                if (block.getEndTime().isAfter(deadline)) {
                    long validMinutes = ChronoUnit.MINUTES.between(block.getStartTime(), deadline);
                    if (validMinutes > 0) {
                        double validHours = validMinutes / MINUTES_PER_HOUR;
                        ScheduleBlock truncated = createBlock(
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
                    double h = ChronoUnit.MINUTES.between(block.getStartTime(), block.getEndTime()) / MINUTES_PER_HOUR;
                    postPlacedHoursPerTask.merge(task, h, Double::sum);
                }
            }

            resultBlocks.addAll(validBlocks);
        }

        // For any pending task where remaining work could not be accommodated before its deadline or schedule end,
        // produce a DELAYED block. If approaching the deadline prevents scheduling, log the error.
        for (Task task : pendingTasks) {
            double required = remainingHoursPerTask.getOrDefault(task, 0.0);
            double placed = postPlacedHoursPerTask.getOrDefault(task, 0.0);
            double unplaced = Math.max(0.0, required - placed);
            if (unplaced > ROUNDING_TOLERANCE) {
                String errorReason;
                if (task.getDeadline() != null) {
                    LocalDate dl = task.getDeadline().toLocalDate();
                    boolean pastOrToday = !unattendedEnd.isBefore(task.getDeadline()) || !resumeDate.isBefore(dl);
                    errorReason = buildUnattendedDeadlineApproachedReason(task, unplaced, dl, pastOrToday);
                    log.error("Rearranging schedule after unattended session approached the day of the deadline ({}) for task '{}': you can't do anything anymore.",
                            dl, task.getTitle());
                } else {
                    errorReason = buildDelayedReason(task, unplaced, endDate);
                }
                resultBlocks.add(createBlock(task, null, null, BlockDecision.DELAYED, errorReason));
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

    public double workingHoursPerDay(UserProfile userProfile) {
        Objects.requireNonNull(userProfile, "userProfile");
        if (userProfile.getPreferredStart() == null || userProfile.getPreferredEnd() == null) {
            throw new IllegalArgumentException("preferredStart and preferredEnd are required");
        }
        double hours = ChronoUnit.MINUTES.between(userProfile.getPreferredStart(), userProfile.getPreferredEnd()) / 60.0;
        if (hours <= 0) {
            throw new IllegalArgumentException("preferredEnd must be after preferredStart");
        }
        return hours;
    }

    /**
     * Orders tasks so the most important and urgent ones come first.
     * If two tasks have equal scores, the one with the earlier deadline is scheduled first.
     */
    // First things first, we sort by the task score which factors in urgency, priority and duration
    // Then, as a secondary approach we then compare deadline in case of 2 tasks having the same score - the one with the earlier deadline comes first
    // If those two are STILL the same, we sort alphabetically.
    private Comparator<Task> taskPlacementComparator(UserProfile userProfile, LocalDate asOfDate) {
        return Comparator
                .comparingDouble((Task t) -> scoringStrategy.scoreTask(t, userProfile, asOfDate)).reversed()
                .thenComparing(Task::getDeadline)
                .thenComparing(t -> t.getId() == null ? "" : t.getId());
    }

    /**
     * Returns the maximum session length for a task.
     * High difficulty tasks (complexity 4 or 5) are limited to 2 hours per session.
     */
    public static double getMaxSessionHours(Task task) {
        return getMaxSessionHoursByComplexity(task.getComplexity());
    }

    public static double getMaxSessionHoursByComplexity(int complexity) {
        if (complexity <= 2) {
            return EASY_SESSION_HOURS;     // 0.5 hours (30 mins)
        } else if (complexity == 3) {
            return MEDIUM_SESSION_HOURS;   // 1.0 hour
        } else {
            return HARD_SESSION_HOURS;     // 2.0 hours
        }
    }

    private ScheduleBlock createBlock(
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
        block.setReason(clampReason(reason));
        block.setManuallyOverridden(false);
        return block;
    }

    private String buildScheduledReason(
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
                .append(", duration=").append(formatHours(scoringStrategy.getEffectiveDurationHours(task))).append("h)");
        if (splitDueToHighDifficulty) {
            explanation.append(". Session split: complexity=")
                    .append(task.getComplexity())
                    .append(" >= ").append(HIGH_COMPLEXITY_THRESHOLD)
                    .append(" so sessions are capped at ")
                    .append(formatHours(MAX_HIGH_COMPLEXITY_SESSION_HOURS)).append("h");
        } else if (decomposedAcrossDays) {
            explanation.append(". Decomposed across days to balance workload and prevent burnout");
        } else if (needsAnotherSession) {
            explanation.append(". Split across days because remaining work exceeds today's free hours");
        }
        return explanation.toString();
    }

    /**
     * Counts how many valid working days remain for a task before its deadline.
     */
    private long countWorkingDaysUntilDeadline(LocalDate currentDay, Task task, LocalDate planEnd, UserProfile profile) {
        LocalDate deadlineDate = task.getDeadline().toLocalDate();
        LocalDate targetEnd = deadlineDate.isBefore(planEnd) ? deadlineDate : planEnd;
        if (currentDay.isAfter(targetEnd)) {
            return 1;
        }

        long workingDays = 0;
        LocalDate d = currentDay;
        while (!d.isAfter(targetEnd)) {
            if (profile.isIncludeWeekends() || !isWeekend(d)) {
                workingDays++;
            }
            if (d.equals(LocalDate.MAX)) {
                break;
            }
            d = d.plusDays(1);
        }
        return Math.max(1, workingDays);
    }

    private String buildDelayedReason(Task task, double unplacedHours, LocalDate scheduleEndDate) {
        return "Delayed: " + formatHours(unplacedHours)
                + "h could not be placed before plan end " + scheduleEndDate
                + " (priority=" + task.getPriority()
                + ", deadline=" + task.getDeadline() + ").";
    }

    public String buildUnattendedDeadlineApproachedReason(Task task, double unplacedHours, LocalDate deadlineDate, boolean isDeadlinePastOrToday) {
        if (isDeadlinePastOrToday) {
            return clampReason("Delayed: Error: Rearranging schedule after unattended session approached the day of the deadline ("
                    + deadlineDate + "). The deadline has already arrived or passed; you can't do anything anymore.");
        }
        return clampReason("Delayed: Error: Rearranging schedule after unattended session approached the day of the deadline ("
                + deadlineDate + "). Remaining " + formatHours(unplacedHours)
                + "h cannot be accommodated before deadline — you can't do anything anymore.");
    }

    /**
     * Checks whether the rearranged blocks contain an error indicating that rearranging after
     * an unattended session led to approaching the deadline and work could no longer be scheduled.
     */
    public boolean hasDeadlineApproachedError(List<ScheduleBlock> blocks) {
        if (blocks == null) return false;
        return blocks.stream()
                .filter(b -> b.getDecision() == BlockDecision.DELAYED)
                .anyMatch(b -> b.getReason() != null && b.getReason().contains(DEADLINE_APPROACHED_ERROR_KEY));
    }

    /**
     * Validates that rearranging after an unattended session did not lead to approaching the deadline.
     * Throws an {@link IllegalStateException} with the descriptive error message if any task was prevented
     * from being scheduled due to approaching the deadline.
     */
    public void validateNoDeadlineApproachedError(List<ScheduleBlock> blocks) {
        if (hasDeadlineApproachedError(blocks)) {
            ScheduleBlock failedBlock = blocks.stream()
                    .filter(b -> b.getDecision() == BlockDecision.DELAYED && b.getReason() != null && b.getReason().contains(DEADLINE_APPROACHED_ERROR_KEY))
                    .findFirst()
                    .orElse(null);
            String msg = failedBlock != null ? failedBlock.getReason() : "Rearranging schedule after unattended session approached the day of the deadline; you can't do anything anymore.";
            throw new IllegalStateException(msg);
        }
    }

    private String buildZeroDurationReason(Task task) {
        return "Delayed: estimated duration is 0h so there is nothing to place"
                + " (priority=" + task.getPriority()
                + ", deadline=" + task.getDeadline() + ").";
    }

    private static LocalDate nextDayOrNull(LocalDate day) {
        if (day.equals(LocalDate.MAX)) {
            return null;
        }
        return day.plusDays(1);
    }

    private static boolean isWeekend(LocalDate day) {
        return day.getDayOfWeek().getValue() >= 6;
    }

    private static String formatHours(double hours) {
        return String.format(java.util.Locale.ROOT, "%.2f", hours);
    }

    public static String clampReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Unspecified scheduling decision";
        }
        if (reason.length() <= MAX_REASON_LENGTH) {
            return reason;
        }
        return reason.substring(0, MAX_REASON_LENGTH - 3) + "...";
    }
}
