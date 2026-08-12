package com.srikrishnanethi.agamotto.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/**
 * Create-task payload. {@code deadline} must be in the future (NEA T14 → HTTP 400).
 */
public record CreateTaskRequest(
		@NotBlank String actorUserId,
		@NotBlank @Size(max = 200) String title,
		@Size(max = 2000) String description,
		@NotBlank @Size(max = 100) String category,
		@Min(1) @Max(5) int priority,
		@NotNull @Future LocalDateTime deadline,
		@Positive double estimatedDurationHours,
		@Min(1) @Max(5) int complexity) {
}
