package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Day-by-day greedy placement used by Serenity and by Crunch survivors.
 *
 * <p>A {@link PriorityQueue} holds ready work ordered by score (highest first). That matters
 * here because a task may be only partly placed, put back on the queue, and later compete again
 * against other unfinished work — a one-off sort would not handle that.
 */
@Component
public class GreedyPlacer {

	public static final int HIGH_COMPLEXITY_THRESHOLD = 4;
	public static final double MAX_HIGH_COMPLEXITY_SESSION_HOURS = 2.0;

	public static final int MAX_REASON_LENGTH = 1000;
	private static final double EPSILON = 1e-9;
	private static final double MINUTES_PER_HOUR = 60.0;

	private final ScoringStrategy scoringStrategy;

	public GreedyPlacer(ScoringStrategy scoringStrategy) {
		this.scoringStrategy = scoringStrategy;
	}

	public List<ScheduleBlock> place(List<Task> tasks, LocalDate start, LocalDate end, UserProfile profile) {
		List<ScheduleBlock> blocks = new ArrayList<>();
		// Nothing to schedule.
		if (tasks == null || tasks.isEmpty()) {
			return blocks;
		}
		Objects.requireNonNull(start, "start");
		Objects.requireNonNull(end, "end");
		Objects.requireNonNull(profile, "profile");

		// Identity map: Task.equals is id-based, so unsaved/proxy instances must not collide.
		Map<Task, Double> remainingHours = new IdentityHashMap<>();
		tasks.forEach(task -> remainingHours.put(task, scoringStrategy.effectiveDurationHours(task)));

		// Highest score first; partially placed tasks are offered back into this queue.
		PriorityQueue<Task> queue = new PriorityQueue<>(taskPlacementComparator(profile, start));
		queue.addAll(tasks);

		// Working-day window from the user's preferences (e.g a regular 9 to 5)
		LocalTime dayStart = profile.getPreferredStart();
		LocalTime dayEnd = profile.getPreferredEnd();
		double hoursPerDay = workingHoursPerDay(profile);

		// Walk each calendar day in the plan window.
		LocalDate day = start;
		while (!day.isAfter(end)) {
			// Honour "weekdays only" if weekends are disabled.
			if (!profile.isIncludeWeekends() && isWeekend(day)) {
				day = nextDayOrNull(day);
				if (day == null) {
					break;
				}
				continue;
			}

			// Fresh budget and clock for this day: fill from dayStart forward.
			double dayHoursLeft = hoursPerDay;
			LocalDateTime cursor = LocalDateTime.of(day, dayStart);
			LocalDateTime dayClose = LocalDateTime.of(day, dayEnd);

			// Keep booking until the day is full or no unfinished tasks remain.
			while (dayHoursLeft > EPSILON && !queue.isEmpty()) {
				// Take the current highest-scoring unfinished task.
				Task task = queue.poll();
				double left = remainingHours.getOrDefault(task, 0.0);
				// Already fully placed (stale queue entry) — skip.
				if (left <= EPSILON) {
					continue;
				}

				if (!cursor.isBefore(dayClose)) {
					queue.offer(task);
					break;
				}

				// Book the largest chunk that fits: day left, task left, and session cap.
				// High-complexity tasks are capped at MAX_HIGH_COMPLEXITY_SESSION_HOURS.
				double placeHours = Math.min(dayHoursLeft, Math.min(left, maxSessionHours(task)));
				long minutes = Math.round(placeHours * MINUTES_PER_HOUR);
				if (minutes <= 0) {
					if (left * MINUTES_PER_HOUR >= 1.0 - EPSILON) {
						// Day leftover is smaller than a calendar minute; try tomorrow.
						queue.offer(task);
						break;
					}
					// Sub-minute remainder cannot be represented; delay it at the end.
					continue;
				}

				LocalDateTime blockEnd = cursor.plusMinutes(minutes);
				// Clamp to preferredEnd if rounding pushed past the working day (including next-day overflow).
				if (blockEnd.isAfter(dayClose)) {
					blockEnd = dayClose;
					minutes = ChronoUnit.MINUTES.between(cursor, blockEnd);
					if (minutes <= 0) {
						queue.offer(task);
						break;
					}
				}
				placeHours = minutes / MINUTES_PER_HOUR;

				// Record a SCHEDULED block for this chunk (with an explanation reason).
				double score = scoringStrategy.scoreTask(task, profile, start);
				boolean split = left > placeHours + EPSILON;
				boolean complexitySplit = split && task.getComplexity() >= HIGH_COMPLEXITY_THRESHOLD;
				blocks.add(createBlock(
						task,
						cursor,
						blockEnd,
						BlockDecision.SCHEDULED,
						buildScheduledReason(task, score, placeHours, day, split, complexitySplit)));

				// Consume the booked hours from the task and from today's budget.
				left -= placeHours;
				dayHoursLeft -= placeHours;
				cursor = blockEnd;
				remainingHours.put(task, left);

				// Unfinished work goes back on the queue to compete again later.
				if (left > EPSILON) {
					queue.offer(task);
				}
			}

			day = nextDayOrNull(day);
			if (day == null) {
				break;
			}
		}

		// Anything still unplaced after the last day becomes DELAYED (kept, but no free slot).
		for (Task task : tasks) {
			double original = scoringStrategy.effectiveDurationHours(task);
			double left = remainingHours.getOrDefault(task, 0.0);
			if (left > EPSILON) {
				blocks.add(createBlock(task, null, null, BlockDecision.DELAYED, buildDelayedReason(task, left, end)));
			} else if (original <= EPSILON) {
				blocks.add(createBlock(task, null, null, BlockDecision.DELAYED, buildZeroDurationReason(task)));
			}
		}

		return blocks;
	}

	public double workingHoursPerDay(UserProfile profile) {
		Objects.requireNonNull(profile, "profile");
		if (profile.getPreferredStart() == null || profile.getPreferredEnd() == null) {
			throw new IllegalArgumentException("preferredStart and preferredEnd are required");
		}
		double hours = ChronoUnit.MINUTES.between(profile.getPreferredStart(), profile.getPreferredEnd()) / 60.0;
		if (hours <= 0) {
			throw new IllegalArgumentException("preferredEnd must be after preferredStart");
		}
		return hours;
	}

	private Comparator<Task> taskPlacementComparator(UserProfile profile, LocalDate asOf) {
		return Comparator
				.comparingDouble((Task t) -> scoringStrategy.scoreTask(t, profile, asOf)).reversed()
				.thenComparing(Task::getDeadline)
				.thenComparing(t -> t.getId() == null ? "" : t.getId());
	}

	private double maxSessionHours(Task task) {
		return task.getComplexity() >= HIGH_COMPLEXITY_THRESHOLD
				? MAX_HIGH_COMPLEXITY_SESSION_HOURS
				: Double.MAX_VALUE;
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
			double score,
			double placeHours,
			LocalDate day,
			boolean split,
			boolean complexitySplit) {
		StringBuilder sb = new StringBuilder();
		sb.append("Scheduled on ").append(day)
				.append(" for ").append(formatHours(placeHours)).append("h")
				.append(" because score=").append(formatHours(score))
				.append(" (priority=").append(task.getPriority())
				.append(", deadline=").append(task.getDeadline())
				.append(", duration=").append(formatHours(scoringStrategy.effectiveDurationHours(task))).append("h)");
		if (complexitySplit) {
			sb.append(". Session split: complexity=")
					.append(task.getComplexity())
					.append(" >= ").append(HIGH_COMPLEXITY_THRESHOLD)
					.append(" so blocks are capped at ")
					.append(formatHours(MAX_HIGH_COMPLEXITY_SESSION_HOURS)).append("h");
		} else if (split) {
			sb.append(". Split across days because remaining work exceeds today's free hours");
		}
		return sb.toString();
	}

	private String buildDelayedReason(Task task, double leftoverHours, LocalDate planEnd) {
		return "Delayed: " + formatHours(leftoverHours)
				+ "h could not be placed before plan end " + planEnd
				+ " (priority=" + task.getPriority()
				+ ", deadline=" + task.getDeadline() + ").";
	}

	private String buildZeroDurationReason(Task task) {
		return "Delayed: estimated duration is 0h so there is nothing to place"
				+ " (priority=" + task.getPriority()
				+ ", deadline=" + task.getDeadline() + ").";
	}

	/** {@code null} when {@code day} is {@link LocalDate#MAX} so {@code plusDays(1)} cannot throw. */
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
