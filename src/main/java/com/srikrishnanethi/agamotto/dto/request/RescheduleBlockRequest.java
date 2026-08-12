package com.srikrishnanethi.agamotto.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Move a single block to an explicit time window. Both times are required;
 * an empty body must not regenerate the parent plan.
 */
public record RescheduleBlockRequest(
		@NotNull LocalDateTime startTime,
		@NotNull LocalDateTime endTime,
		@Size(max = 1000) String reason) {
}
