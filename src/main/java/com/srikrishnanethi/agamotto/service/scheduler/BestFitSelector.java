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
			return new BestFitResult(List.copyOf(tasks), List.of());
		}

		List<Task> candidates = new ArrayList<>(tasks);
		candidates.sort(Comparator
				.comparingInt(Task::getPriority)
				.thenComparingDouble(scoringStrategy::effectiveDurationHours)
				.thenComparing(t -> t.getId() == null ? "" : t.getId()));

		List<Task> excluded = new ArrayList<>();
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
