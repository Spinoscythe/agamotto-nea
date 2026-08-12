package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.Task;

import java.util.List;

/**
 * Consistent survivor/excluded pair produced by {@link BestFitSelector#select}.
 */
public record BestFitResult(List<Task> remaining, List<Task> excluded) {
}
