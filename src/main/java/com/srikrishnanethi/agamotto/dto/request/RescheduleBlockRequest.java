package com.srikrishnanethi.agamotto.dto.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Move a single block when times are set; otherwise regenerate the parent project's plan.
 */
public record RescheduleBlockRequest(
		LocalDateTime startTime,
		LocalDateTime endTime,
		@Size(max = 1000) String reason) {
}
