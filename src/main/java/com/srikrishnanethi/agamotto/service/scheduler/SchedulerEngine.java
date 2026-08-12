package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import com.srikrishnanethi.agamotto.entities.enums.ScheduleMode;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Core scheduling algorithms from NEA §2.10 / §2.11.
 *
 * <p>Chooses Serenity or Crunch, then delegates scoring, best-fit exclusion, and placement.
 *
 * <h2>Session splitting (Serenity / §2.10)</h2>
 * Tasks with {@code complexity >= HIGH_COMPLEXITY_THRESHOLD} are split into sessions of at most
 * {@value #MAX_HIGH_COMPLEXITY_SESSION_HOURS} hours.
 */
@Service
public class SchedulerEngine {

	public static final int HIGH_COMPLEXITY_THRESHOLD = GreedyPlacer.HIGH_COMPLEXITY_THRESHOLD;
	public static final double MAX_HIGH_COMPLEXITY_SESSION_HOURS = GreedyPlacer.MAX_HIGH_COMPLEXITY_SESSION_HOURS;

	private static final double EPSILON = 1e-9;

	private final ScoringStrategy scoringStrategy;
	private final BestFitSelector bestFitSelector;
	private final GreedyPlacer greedyPlacer;

	public SchedulerEngine(
			ScoringStrategy scoringStrategy,
			BestFitSelector bestFitSelector,
			GreedyPlacer greedyPlacer) {
		this.scoringStrategy = scoringStrategy;
		this.bestFitSelector = bestFitSelector;
		this.greedyPlacer = greedyPlacer;
	}

	/** Serenity when total hours fit capacity; otherwise Crunch. */
	public ScheduleMode selectMode(List<Task> tasks, LocalDate start, LocalDate end, UserProfile profile) {
		double total = sumEffectiveHours(tasks);
		double available = availableHours(start, end, profile);
		return total <= available + EPSILON ? ScheduleMode.SERENITY : ScheduleMode.CRUNCH;
	}

	public ScheduleResult generate(List<Task> tasks, LocalDate start, LocalDate end, UserProfile profile) {
		if (selectMode(tasks, start, end, profile) == ScheduleMode.SERENITY) {
			return runSerenity(tasks, start, end, profile);
		}
		return runCrunch(tasks, start, end, profile);
	}

	/** Fit workload: place every task by score, day by day. */
	public ScheduleResult runSerenity(List<Task> tasks, LocalDate start, LocalDate end, UserProfile profile) {
		Objects.requireNonNull(tasks, "tasks");
		validateRange(start, end);
		Objects.requireNonNull(profile, "profile");

		List<ScheduleBlock> blocks = greedyPlacer.place(tasks, start, end, profile);
		String summary = "Serenity mode: workload fits available hours ("
				+ formatHours(sumEffectiveHours(tasks)) + "h <= "
				+ formatHours(availableHours(start, end, profile)) + "h). "
				+ "Tasks ordered by score (priority, deadline urgency, duration) with deadline tie-break.";
		return new ScheduleResult(ScheduleMode.SERENITY, summary, List.copyOf(blocks));
	}

	/**
	 * Overload: order by earliest deadline, drop lower-priority / shorter tasks until the rest
	 * fits, place survivors, mark the rest EXCLUDED.
	 */
	public ScheduleResult runCrunch(List<Task> tasks, LocalDate start, LocalDate end, UserProfile profile) {
		Objects.requireNonNull(tasks, "tasks");
		validateRange(start, end);
		Objects.requireNonNull(profile, "profile");

		List<Task> byDeadline = new ArrayList<>(tasks);
		byDeadline.sort(Comparator
				.comparing(Task::getDeadline)
				.thenComparing(t -> t.getId() == null ? "" : t.getId()));

		double available = availableHours(start, end, profile);
		BestFitResult fit = bestFit(byDeadline, available);

		List<ScheduleBlock> blocks = new ArrayList<>(greedyPlacer.place(fit.remaining(), start, end, profile));
		for (Task excluded : fit.excluded()) {
			blocks.add(createBlock(
					excluded,
					null,
					null,
					BlockDecision.EXCLUDED,
					buildExcludedReason(excluded, available, sumEffectiveHours(tasks))));
		}

		String summary = "Crunch mode: workload ("
				+ formatHours(sumEffectiveHours(tasks)) + "h) exceeds available hours ("
				+ formatHours(available) + "h). "
				+ "Best-fit excluded " + fit.excluded().size()
				+ " lower-priority / shorter task(s); survivors placed by score.";
		return new ScheduleResult(ScheduleMode.CRUNCH, summary, List.copyOf(blocks));
	}

	public BestFitResult bestFit(List<Task> tasks, double availableHours) {
		return bestFitSelector.select(tasks, availableHours);
	}

	public double availableHours(LocalDate start, LocalDate end, UserProfile profile) {
		validateRange(start, end);
		long days = 0;
		for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
			if (profile.isIncludeWeekends() || !isWeekend(day)) {
				days++;
			}
		}
		return days * workingHoursPerDay(profile);
	}

	public double effectiveDurationHours(Task task) {
		return scoringStrategy.effectiveDurationHours(task);
	}

	public double deadlineUrgency(Task task, LocalDate asOf) {
		return scoringStrategy.deadlineUrgency(task, asOf);
	}

	public double workingHoursPerDay(UserProfile profile) {
		return greedyPlacer.workingHoursPerDay(profile);
	}

	private static boolean isWeekend(LocalDate day) {
		return day.getDayOfWeek().getValue() >= 6;
	}

	private double sumEffectiveHours(List<Task> tasks) {
		double sum = 0.0;
		for (Task task : tasks) {
			sum += scoringStrategy.effectiveDurationHours(task);
		}
		return sum;
	}

	private ScheduleBlock createBlock(
			Task task,
			java.time.LocalDateTime start,
			java.time.LocalDateTime end,
			BlockDecision decision,
			String reason) {
		ScheduleBlock block = new ScheduleBlock();
		block.setTask(task);
		block.setStartTime(start);
		block.setEndTime(end);
		block.setDecision(decision);
		block.setReason(reason);
		block.setManuallyOverridden(false);
		return block;
	}

	private String buildExcludedReason(Task task, double availableHours, double totalHours) {
		return "Excluded by best-fit: priority=" + task.getPriority()
				+ ", duration=" + formatHours(scoringStrategy.effectiveDurationHours(task)) + "h"
				+ ", deadline urgency relative to overflow"
				+ " (total=" + formatHours(totalHours) + "h > available="
				+ formatHours(availableHours) + "h). Lower priority / shorter tasks removed first.";
	}

	private void validateRange(LocalDate start, LocalDate end) {
		Objects.requireNonNull(start, "start");
		Objects.requireNonNull(end, "end");
		if (end.isBefore(start)) {
			throw new IllegalArgumentException("end must be on or after start");
		}
	}

	private static String formatHours(double hours) {
		return String.format(java.util.Locale.ROOT, "%.2f", hours);
	}
}
