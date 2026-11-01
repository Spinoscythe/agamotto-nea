package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.TaskDependency;
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
 * {@value #ScheduleBuilder.MAX_HIGH_COMPLEXITY_SESSION_HOURS} hours.
 *
 * <h2>Why these data structures</h2>
 * <ul>
 *   <li>{@code ArrayList} copies before {@code sort} — the input {@code List} is often a
 *       Hibernate-backed collection. Sorting it in place would mutate the persistence
 *       context.</li>
 *   <li>Crunch deadline list is an {@code ArrayList} so Timsort can produce a full ordered
 *       snapshot for {@link BestFitSelector}. A {@code PriorityQueue} would only give
 *       poll-order, not a random-access list.</li>
 *   <li>{@code ArrayList} of blocks — greedy output is copied so EXCLUDED rows can be
 *       appended without mutating the placer's list. {@link List#copyOf} then freezes
 *       the result for persist.</li>
 * </ul>
 */
@Service
public class SchedulerEngine {

	public static final int HIGH_COMPLEXITY_THRESHOLD = ScheduleBuilder.HIGH_COMPLEXITY_THRESHOLD;
	public static final double MAX_HIGH_COMPLEXITY_SESSION_HOURS = ScheduleBuilder.MAX_HIGH_COMPLEXITY_SESSION_HOURS;
	/** Inclusive calendar-day cap so {@code LocalDate.MAX} / huge ranges cannot hang or throw. */
	public static final int MAX_PLAN_DAYS = 366 * 2;

	private static final double EPSILON = 1e-9;

	private final ScoringStrategy scoringStrategy;
	private final BestFitSelector bestFitSelector;
	private final ScheduleBuilder scheduleBuilder;
	private final DependencyGraphService dependencyGraph;

	public SchedulerEngine(
			ScoringStrategy scoringStrategy,
			BestFitSelector bestFitSelector,
			ScheduleBuilder scheduleBuilder,
			DependencyGraphService dependencyGraph) {
		this.scoringStrategy = scoringStrategy;
		this.bestFitSelector = bestFitSelector;
		this.scheduleBuilder = scheduleBuilder;
		this.dependencyGraph = dependencyGraph;
	}

	public ScheduleBuilder getScheduleBuilder() {
		return scheduleBuilder;
	}

	/** Serenity when total hours fit capacity; otherwise Crunch. */
	public ScheduleMode selectMode(List<Task> tasks, LocalDate start, LocalDate end, UserProfile profile) {
		Objects.requireNonNull(tasks, "tasks");
		Objects.requireNonNull(profile, "profile");
		double total = SchedulerEngineHelpers.sumEffectiveHours(tasks, scoringStrategy);
		double available = availableHours(start, end, profile);
		return total <= available + EPSILON ? ScheduleMode.SERENITY : ScheduleMode.CRUNCH;
	}

	/**
	 * Generate a schedule without dependency awareness (legacy path).
	 * Delegates to the dependency-aware version with an empty graph.
	 */
	public ScheduleResult generate(List<Task> tasks, LocalDate start, LocalDate end, UserProfile profile,
			List<TaskDependency> dependencyEdges) {
		Objects.requireNonNull(tasks, "tasks");
		Objects.requireNonNull(profile, "profile");
		SchedulerEngineHelpers.validateRange(start, end, MAX_PLAN_DAYS);
		SchedulerEngineHelpers.validateProfile(profile, scheduleBuilder);
		SchedulerEngineHelpers.validateTasks(tasks, scoringStrategy);

		if (dependencyEdges != null && !dependencyEdges.isEmpty()) {
			if (dependencyGraph.hasCycle(tasks, dependencyEdges)) {
				throw new IllegalStateException("Task dependencies contain a cycle: "
						+ dependencyGraph.findCyclePath(tasks, dependencyEdges));
			}
		}

		List<TaskDependency> edges = dependencyEdges != null ? dependencyEdges : List.of();

		if (selectMode(tasks, start, end, profile) == ScheduleMode.SERENITY) {
			return runSerenity(tasks, start, end, profile, edges);
		}
		return runCrunch(tasks, start, end, profile, edges);
	}

	/** Legacy entry point — keeps existing callers working. */
	public ScheduleResult generate(List<Task> tasks, LocalDate start, LocalDate end, UserProfile profile) {
		return generate(tasks, start, end, profile, List.of());
	}

	/** Fit workload: place every task by score, day by day. */
	public ScheduleResult runSerenity(List<Task> tasks, LocalDate start, LocalDate end, UserProfile profile,
			List<TaskDependency> dependencyEdges) {
		Objects.requireNonNull(tasks, "tasks");
		SchedulerEngineHelpers.validateRange(start, end, MAX_PLAN_DAYS);
		Objects.requireNonNull(profile, "profile");
		SchedulerEngineHelpers.validateProfile(profile, scheduleBuilder);
		SchedulerEngineHelpers.validateTasks(tasks, scoringStrategy);

		List<ScheduleBlock> blocks = scheduleBuilder.buildSchedule(tasks, start, end, profile,
				null, null, dependencyEdges);
		String summary = "Serenity mode: workload fits available hours ("
				+ SchedulerEngineHelpers.formatHours(
						SchedulerEngineHelpers.sumEffectiveHours(tasks, scoringStrategy)) + "h <= "
				+ SchedulerEngineHelpers.formatHours(availableHours(start, end, profile)) + "h). "
				+ "Tasks ordered by score (priority, deadline urgency, duration) with deadline tie-break.";
		return new ScheduleResult(ScheduleMode.SERENITY, summary, List.copyOf(blocks));
	}

	/**
	 * Overload: order by earliest deadline, drop lower-priority / shorter tasks until the rest
	 * fits, place survivors, mark the rest EXCLUDED.
	 */
	public ScheduleResult runCrunch(List<Task> tasks, LocalDate start, LocalDate end, UserProfile profile,
			List<TaskDependency> dependencyEdges) {
		Objects.requireNonNull(tasks, "tasks");
		SchedulerEngineHelpers.validateRange(start, end, MAX_PLAN_DAYS);
		Objects.requireNonNull(profile, "profile");
		SchedulerEngineHelpers.validateProfile(profile, scheduleBuilder);
		SchedulerEngineHelpers.validateTasks(tasks, scoringStrategy);

		// Copy-then-sort: do not reorder the JPA list the service passed in.
		List<Task> byDeadline = new ArrayList<>(tasks);
		byDeadline.sort(Comparator
				.comparing(Task::getDeadline)
				.thenComparing(t -> t.getId() == null ? "" : t.getId()));

		double available = availableHours(start, end, profile);
		if (available <= EPSILON) {
			return SchedulerEngineHelpers.emptyWindowResult(tasks, profile);
		}

		BestFitResult fit = bestFit(byDeadline, available);

		// Mutable copy of greedy output so EXCLUDED rows can be appended.
		List<ScheduleBlock> blocks = new ArrayList<>(scheduleBuilder.buildSchedule(
				fit.remaining(), start, end, profile, null, null, dependencyEdges));
		for (Task excluded : fit.excluded()) {
			blocks.add(SchedulerEngineHelpers.createBlock(
					excluded,
					null,
					null,
					BlockDecision.EXCLUDED,
					SchedulerEngineHelpers.buildExcludedReason(
							excluded,
							available,
							SchedulerEngineHelpers.sumEffectiveHours(tasks, scoringStrategy),
							scoringStrategy)));
		}

		String summary = "Crunch mode: workload ("
				+ SchedulerEngineHelpers.formatHours(
						SchedulerEngineHelpers.sumEffectiveHours(tasks, scoringStrategy)) + "h) exceeds available hours ("
				+ SchedulerEngineHelpers.formatHours(available) + "h). "
				+ "Best-fit excluded " + fit.excluded().size()
				+ " lower-priority / shorter task(s); survivors placed by score.";
		return new ScheduleResult(ScheduleMode.CRUNCH, summary, List.copyOf(blocks));
	}

	public BestFitResult bestFit(List<Task> tasks, double availableHours) {
		return bestFitSelector.select(tasks, availableHours);
	}

	public double availableHours(LocalDate start, LocalDate end, UserProfile profile) {
		SchedulerEngineHelpers.validateRange(start, end, MAX_PLAN_DAYS);
		Objects.requireNonNull(profile, "profile");
		long days = 0;
		LocalDate day = start;
		while (!day.isAfter(end)) {
			if (profile.worksOnWeekends() || !SchedulerEngineHelpers.isWeekend(day)) {
				days++;
			}
			if (day.equals(LocalDate.MAX)) {
				break;
			}
			day = day.plusDays(1);
		}
		return days * workingHoursPerDay(profile);
	}

	public double effectiveDurationHours(Task task) {
		return scoringStrategy.getEffectiveDurationHoursFor(task);
	}

	public double deadlineUrgency(Task task, LocalDate asOf) {
		return scoringStrategy.deadlineUrgency(task, asOf);
	}

	public double workingHoursPerDay(UserProfile profile) {
		return scheduleBuilder.workingHoursPerDay(profile);
	}
}
