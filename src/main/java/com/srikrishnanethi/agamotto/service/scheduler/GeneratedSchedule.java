package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.SchedulePlan;

/**
 * Persisted schedule plus the engine's plan-level explanation (not stored on the entity).
 */
public record GeneratedSchedule(SchedulePlan plan, String explanationSummary) {
}
