package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.Task;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Best-fit exclusion: drop lower-priority / shorter tasks until overflow is covered.
 *
 * <h2>Why these data structures</h2>
 * <ul>
 *   <li>{@code ArrayList} copy of the input — {@link List#sort} mutates in place. The engine
 *       still needs the caller's list, so we copy first. ArrayList Timsort is O(n log n),
 *       which is fine for a project's task list.</li>
 *   <li>Two views of exclusions: an {@code ArrayList} (drop order, so we can restore the
 *       highest-priority victim if we emptied the window) plus a {@code HashSet} for O(1)
 *       {@code contains} when rebuilding remaining in original order. {@code ArrayList.contains}
 *       would be O(n) per task (O(n²) overall). A {@code TreeSet} would need a comparator
 *       and would not keep drop order.</li>
 *   <li>{@link List#copyOf} on the result — an immutable snapshot so later placement cannot
 *       mutate the selector's lists.</li>
 * </ul>
 */
@Component
public class BestFitSelector {

	private static final double EPSILON = 1e-9;

	private final ScoringStrategy scoringStrategy;

	public BestFitSelector(ScoringStrategy scoringStrategy) {
		this.scoringStrategy = scoringStrategy;
	}

	/**
	 * Sort by priority then duration (lowest first), drop tasks until overflow is covered.
	 * When there is no overflow, remaining is the full input and excluded is empty.
	 */
	public BestFitResult select(List<Task> tasks, double availableHours) {
		Objects.requireNonNull(tasks, "tasks");
		if (availableHours < 0) {
			throw new IllegalArgumentException("availableHours must be >= 0");
		}

		double overflow = sumEffectiveHours(tasks) - availableHours;
		if (overflow <= EPSILON) {
			// Immutable copy: caller must not see a live view of the JPA collection.
			return new BestFitResult(List.copyOf(tasks), List.of());
		}

		// Mutable copy so sort does not reorder the engine's input list.
		List<Task> candidates = new ArrayList<>(tasks);
		candidates.sort(Comparator
				.comparingInt(Task::getPriority)
				.thenComparingDouble(scoringStrategy::effectiveDurationHours)
				.thenComparing(t -> t.getId() == null ? "" : t.getId()));

		List<Task> excluded = new ArrayList<>();
		// O(1) membership while scanning the original list for survivors.
		Set<Task> excludedSet = new HashSet<>();
		double removedHours = 0.0;
		for (Task task : candidates) {
			if (removedHours >= overflow - EPSILON) {
				break;
			}
			excluded.add(task);
			excludedSet.add(task);
			removedHours += scoringStrategy.effectiveDurationHours(task);
		}

		// Rebuild remaining in the caller's order (not the cheap-first sort order).
		List<Task> remaining = new ArrayList<>();
		for (Task task : tasks) {
			if (!excludedSet.contains(task)) {
				remaining.add(task);
			}
		}

		// If every task was dropped but the window still has hours, keep the
		// highest-priority excluded task so greedy can place a prefix (DELAYED rest).
		if (remaining.isEmpty() && availableHours > EPSILON && !excluded.isEmpty()) {
			Task restore = excluded.stream()
					.max(Comparator.comparingInt(Task::getPriority)
							.thenComparingDouble(scoringStrategy::effectiveDurationHours)
							.thenComparing(t -> t.getId() == null ? "" : t.getId()))
					.orElse(null);
			if (restore != null) {
				// ArrayList.remove is O(n); n is the handful of dropped tasks, not a concern.
				excluded.remove(restore);
				remaining.add(restore);
			}
		}

		return new BestFitResult(List.copyOf(remaining), List.copyOf(excluded));
	}

	private double sumEffectiveHours(List<Task> tasks) {
		double sum = 0.0;
		for (Task task : tasks) {
			sum += scoringStrategy.effectiveDurationHours(task);
		}
		return sum;
	}
}
