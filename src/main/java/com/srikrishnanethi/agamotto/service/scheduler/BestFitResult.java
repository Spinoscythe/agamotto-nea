package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.Task;

import java.util.List;

/**
 * Consistent survivor/excluded pair produced by {@link BestFitSelector#select}.
 *
 * <p>Two lists rather than a {@code Map<Task, Boolean>}: callers iterate survivors and
 * excluded separately, and drop-order vs original-order is preserved by the selector.
 * Each list is a {@link List#copyOf} snapshot so later placement cannot mutate them.
 */
public record BestFitResult(List<Task> remaining, List<Task> excluded) {
}
