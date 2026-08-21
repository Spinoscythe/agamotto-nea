package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BestFitSelectorTest {

	private final BestFitSelector selector = new BestFitSelector(new ScoringStrategy());

	@Test
	void keepsHighestPriorityTaskWhenEveryTaskExceedsWindow() {
		List<Task> tasks = List.of(
				task("low", 1, 20),
				task("high", 5, 20),
				task("mid", 3, 20));

		BestFitResult result = selector.select(tasks, 8.0);

		assertEquals(1, result.remaining().size());
		assertEquals("high", result.remaining().getFirst().getId());
		assertEquals(2, result.excluded().size());
		assertTrue(result.excluded().stream().noneMatch(t -> "high".equals(t.getId())));
	}

	private static Task task(String id, int priority, double hours) {
		Task task = new Task();
		task.setId(id);
		task.setPriority(priority);
		task.setEstimatedDurationHours(hours);
		task.setComplexity(1);
		task.setDeadline(LocalDateTime.of(2026, 8, 30, 17, 0));
		return task;
	}
}
