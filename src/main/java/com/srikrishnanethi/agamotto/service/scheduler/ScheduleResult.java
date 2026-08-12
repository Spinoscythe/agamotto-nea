package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.enums.ScheduleMode;

import java.util.List;

/**
 * In-memory output of the scheduling engine (not yet persisted).
 * Phase 3 attaches blocks to a {@code SchedulePlan} inside a transaction.
 */
public record ScheduleResult(
		ScheduleMode mode,
		String explanationSummary,
		List<ScheduleBlock> blocks
) {
}
