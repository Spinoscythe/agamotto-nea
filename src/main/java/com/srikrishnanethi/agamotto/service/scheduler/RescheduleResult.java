package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;

/**
 * Either a single moved block, or a fully regenerated schedule (exactly one is non-null).
 */
public record RescheduleResult(ScheduleBlock movedBlock, GeneratedSchedule regenerated) {
}
