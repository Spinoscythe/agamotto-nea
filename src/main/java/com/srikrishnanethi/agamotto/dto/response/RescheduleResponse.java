package com.srikrishnanethi.agamotto.dto.response;

/**
 * Response for reschedule endpoints.
 * Exactly one of {@code block} / {@code plan} is populated depending on {@code mode}.
 */
public record RescheduleResponse(
		String mode,
		ScheduleBlockResponse block,
		SchedulePlanResponse plan) {
}
