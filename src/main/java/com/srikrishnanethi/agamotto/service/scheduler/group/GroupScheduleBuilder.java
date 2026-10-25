package com.srikrishnanethi.agamotto.service.scheduler.group;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import com.srikrishnanethi.agamotto.service.scheduler.ScheduleBuilder;
import com.srikrishnanethi.agamotto.service.scheduler.ScoringStrategy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Schedules group tasks onto member calendars using admin assignments and pacing.
 */
@Component
public class GroupScheduleBuilder {

    private static final double EPSILON = 1e-9;
    private static final double MINUTES_PER_HOUR = 60.0;

    private final ScoringStrategy scoringStrategy;

    public GroupScheduleBuilder(ScoringStrategy scoringStrategy) {
        this.scoringStrategy = scoringStrategy;
    }

    public List<ScheduleBlock> buildSchedule(
            List<Task> tasks,
            LocalDate startDay,
            LocalDate endDay,
            Map<User, UserProfile> teamProfiles) {

        List<ScheduleBlock> scheduledBlocks = new ArrayList<>();
        if (tasks == null || tasks.isEmpty() || teamProfiles.isEmpty()) {
            return scheduledBlocks;
        }

        // 1. Create a schedule tracker for each team member
        Map<String, MemberSchedule> memberSchedules = new LinkedHashMap<>();
        for (Map.Entry<User, UserProfile> entry : teamProfiles.entrySet()) {
            User user = entry.getKey();
            UserProfile profile = entry.getValue();
            double totalHours = calculateTotalWorkingHours(startDay, endDay, profile);
            memberSchedules.put(user.getId(), new MemberSchedule(user, profile, startDay, totalHours));
        }

        // 2. Track remaining hours for each task using object memory address
        Map<Task, Double> unfinishedHours = new IdentityHashMap<>();
        for (Task task : tasks) {
            unfinishedHours.put(task, scoringStrategy.getEffectiveDurationHours(task));
        }

        // 3. Step day-by-day through the calendar
        Set<Task> tasksForTomorrow = new LinkedHashSet<>(tasks);
        LocalDate currentDay = startDay;
        while (!currentDay.isAfter(endDay) && !tasksForTomorrow.isEmpty()) {

            // Reset each team member's clock to their morning start time
            for (MemberSchedule member : memberSchedules.values()) {
                if (member.getProfile().isIncludeWeekends() || !isWeekend(currentDay)) {
                    member.startNewDay(currentDay);
                } else {
                    // Weekend: Mark day as booked if member does not work weekends
                    member.startNewDay(currentDay);
                    member.bookTime(member.getNextFreeTime(), member.getFreeHoursToday());
                }
            }

            // Start of day: query tasksForTomorrow to obtain today's workload
            Set<Task> tasksForToday = new LinkedHashSet<>(tasksForTomorrow);
            tasksForTomorrow.clear();

            boolean anyMemberCanWorkToday = memberSchedules.values().stream().anyMatch(m -> m.getFreeHoursToday() > EPSILON);
            if (!anyMemberCanWorkToday) {
                tasksForTomorrow.addAll(tasksForToday);
                currentDay = currentDay.plusDays(1);
                continue;
            }

            // Put unfinished tasks into a priority queue (highest priority first, then earliest deadline, then alphabetically)
            PriorityQueue<Task> taskQueue = new PriorityQueue<>(Comparator
                    .comparingInt(Task::getPriority).reversed()
                    .thenComparing(Task::getDeadline)
                    .thenComparing(t -> t.getId() == null ? "" : t.getId()));

            for (Task t : tasksForToday) {
                if (unfinishedHours.getOrDefault(t, 0.0) > EPSILON) {
                    taskQueue.add(t);
                }
            }

            List<Task> tasksForTodayNextRound = new ArrayList<>();
            Map<Task, Double> hoursPlacedToday = new IdentityHashMap<>();

            // Place tasks into available calendar slots today
            while (!taskQueue.isEmpty() || !tasksForTodayNextRound.isEmpty()) {
                // Query tasksForTodayNextRound when current round finishes
                if (taskQueue.isEmpty()) {
                    taskQueue.addAll(tasksForTodayNextRound);
                    tasksForTodayNextRound.clear();
                }

                Task task = taskQueue.poll();
                double hoursLeftOnTask = unfinishedHours.getOrDefault(task, 0.0);
                if (hoursLeftOnTask <= EPSILON) {
                    continue; // Task is already completed
                }

                // Route to admin-assigned member, or balance if unassigned
                MemberSchedule bestMember;
                if (Objects.requireNonNull(task).getAssignee() != null && memberSchedules.containsKey(task.getAssignee().getId())) {
                    MemberSchedule assignedMember = memberSchedules.get(task.getAssignee().getId());
                    if (assignedMember.getFreeHoursToday() > EPSILON) {
                        bestMember = assignedMember;
                    } else {
                        // Assigned member has no free time left today; defer to tomorrow
                        tasksForTomorrow.add(task);
                        continue;
                    }
                } else {
                    bestMember = findLeastBusyAvailableMember(memberSchedules.values());
                }

                if (bestMember == null || bestMember.getFreeHoursToday() <= EPSILON) {
                    tasksForTomorrow.add(task);
                    break;
                }

                // Decompose task across working days before deadline to avoid fatigue
                long daysRemaining = countWorkingDaysUntilDeadline(currentDay, task, endDay, bestMember.getProfile());
                double maxSittingHours = maxHoursPerSitting(task.getComplexity());
                double alreadyPlacedToday = hoursPlacedToday.getOrDefault(task, 0.0);

                double dailyQuota;
                if (daysRemaining <= 1) {
                    dailyQuota = hoursLeftOnTask;
                } else {
                    double dailyPace = hoursLeftOnTask / daysRemaining;
                    dailyQuota = Math.max(0.5, Math.ceil(dailyPace * 2.0) / 2.0);
                }

                double quotaLeftToday = Math.max(0.0, dailyQuota - alreadyPlacedToday);
                if (daysRemaining > 1 && quotaLeftToday <= EPSILON) {
                    // Today's quota reached; defer remainder to tomorrow
                    tasksForTomorrow.add(task);
                    continue;
                }

                double targetSessionHours = Math.min(hoursLeftOnTask, maxSittingHours);
                if (daysRemaining > 1) {
                    targetSessionHours = Math.min(targetSessionHours, quotaLeftToday);
                }

                // If this sitting doesn't fit in the member's remaining time today, try tomorrow
                if (targetSessionHours > bestMember.getFreeHoursToday() + EPSILON
                        && hoursLeftOnTask > bestMember.getFreeHoursToday() + EPSILON) {
                    tasksForTomorrow.add(task);
                    continue;
                }

                // Calculate the actual hours and minutes to book
                double hoursToBook = Math.min(targetSessionHours, bestMember.getFreeHoursToday());
                long minutesToBook = Math.round(hoursToBook * MINUTES_PER_HOUR);
                if (minutesToBook <= 0) {
                    tasksForTomorrow.add(task);
                    continue;
                }

                LocalDateTime slotStart = bestMember.getNextFreeTime();
                LocalDateTime slotEnd = slotStart.plusMinutes(minutesToBook);

                boolean isTaskSplit = hoursLeftOnTask > hoursToBook + EPSILON;
                boolean isSessionCapped = isTaskSplit && hoursToBook >= maxSittingHours - EPSILON;
                boolean isDecomposedAcrossDays = isTaskSplit && daysRemaining > 1;

                String explanation = makeSlotExplanation(
                        task, bestMember.getUser(), bestMember.getBusyPercentage(),
                        hoursToBook, currentDay, isTaskSplit, isSessionCapped, maxSittingHours, isDecomposedAcrossDays);

                // Create the scheduled calendar block
                ScheduleBlock block = new ScheduleBlock();
                block.setTask(task);
                block.setStartTime(slotStart);
                block.setEndTime(slotEnd);
                block.setDecision(BlockDecision.SCHEDULED);
                block.setReason(ScheduleBuilder.clampReason(explanation));
                block.setManuallyOverridden(false);
                scheduledBlocks.add(block);

                // Update member's clock and deduct the booked hours from the task
                bestMember.bookTime(slotEnd, hoursToBook);
                hoursLeftOnTask -= hoursToBook;
                unfinishedHours.put(task, hoursLeftOnTask);
                hoursPlacedToday.put(task, alreadyPlacedToday + hoursToBook);

                // Re-queue remaining work: into today's next round if quota remains, or tomorrow if quota is reached
                if (hoursLeftOnTask > EPSILON) {
                    double newPlacedToday = alreadyPlacedToday + hoursToBook;
                    if (daysRemaining > 1 && newPlacedToday >= dailyQuota - EPSILON) {
                        tasksForTomorrow.add(task);
                    } else {
                        tasksForTodayNextRound.add(task);
                    }
                }
            }

            // Carry over any unplaced tasks from today's queues into tomorrow
            tasksForTomorrow.addAll(taskQueue);
            tasksForTomorrow.addAll(tasksForTodayNextRound);
            taskQueue.clear();
            tasksForTodayNextRound.clear();

            currentDay = currentDay.plusDays(1);
        }

        // 4. Any tasks with unfinished hours after the last calendar day are marked
        // DELAYED
        for (Task task : tasks) {
            double hoursLeft = unfinishedHours.getOrDefault(task, 0.0);
            if (hoursLeft > EPSILON) {
                ScheduleBlock delayedBlock = new ScheduleBlock();
                delayedBlock.setTask(task);
                delayedBlock.setDecision(BlockDecision.DELAYED);
                delayedBlock.setReason(ScheduleBuilder.clampReason("Delayed: "
                        + String.format(Locale.ROOT, "%.2f", hoursLeft)
                        + " hours could not fit within the team's working hours before " + endDay));
                scheduledBlocks.add(delayedBlock);
            }
        }

        return scheduledBlocks;
    }

    // Counts remaining working days for a task before its deadline
    private long countWorkingDaysUntilDeadline(LocalDate currentDay, Task task, LocalDate planEnd,
            UserProfile profile) {
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

    // Finds available member with lowest current workload
    private MemberSchedule findLeastBusyAvailableMember(Collection<MemberSchedule> members) {
        return members.stream()
                .filter(m -> m.getFreeHoursToday() > EPSILON)
                .min(Comparator.comparingDouble(MemberSchedule::getBusyPercentage))
                .orElse(null);
    }

    // Maximum hours per sitting by task complexity
    private double maxHoursPerSitting(int complexity) {
        if (complexity <= 2)
            return 0.5;
        if (complexity == 3)
            return 1.0;
        return 2.0;
    }

    // Calculates total working hours for a member within the date range
    private double calculateTotalWorkingHours(LocalDate start, LocalDate end, UserProfile profile) {
        long workingDays = 0;
        LocalDate day = start;
        while (!day.isAfter(end)) {
            if (profile.isIncludeWeekends() || !isWeekend(day)) {
                workingDays++;
            }
            day = day.plusDays(1);
        }
        double dailyHours = ChronoUnit.MINUTES.between(profile.getPreferredStart(), profile.getPreferredEnd()) / 60.0;
        return workingDays * Math.max(0.0, dailyHours);
    }

    private static boolean isWeekend(LocalDate day) {
        return day.getDayOfWeek().getValue() >= 6;
    }

    // Explains why the time block was scheduled
    private String makeSlotExplanation(
            Task task, User member, double busyPercentage, double hours,
            LocalDate day, boolean wasSplit, boolean wasCapped, double cap, boolean wasDecomposed) {

        String name = member.getFullName() != null ? member.getFullName() : "Member";
        StringBuilder text = new StringBuilder();
        text.append("Scheduled on ").append(day).append(" for ").append(name)
                .append(" (currently ").append(String.format(Locale.ROOT, "%.0f%%", busyPercentage * 100.0))
                .append(" busy)")
                .append(" for ").append(String.format(Locale.ROOT, "%.2f", hours)).append("h");

        if (task.getAssignee() != null) {
            text.append(". Assigned by project admin");
        }

        if (wasCapped) {
            text.append(". Limited to a ").append(String.format(Locale.ROOT, "%.1f", cap))
                    .append("h sitting (Complexity ").append(task.getComplexity()).append(")");
        } else if (wasDecomposed) {
            text.append(". Decomposed across days to balance team workload and prevent burnout");
        } else if (wasSplit) {
            text.append(". Split across days because remaining work exceeds today's free hours");
        }
        return text.toString();
    }
}