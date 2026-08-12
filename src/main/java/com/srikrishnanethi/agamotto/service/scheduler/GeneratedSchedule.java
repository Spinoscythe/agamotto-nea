package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.SchedulePlan;

/**
 * Persisted schedule plus the engine's plan-level explanation (also stored on the plan).
 */
public record GeneratedSchedule(SchedulePlan plan, String explanationSummary) {
}
