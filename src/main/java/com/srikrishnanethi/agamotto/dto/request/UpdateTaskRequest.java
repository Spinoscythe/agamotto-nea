package com.srikrishnanethi.agamotto.dto.request;

import com.srikrishnanethi.agamotto.entities.enums.TaskStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateTaskRequest(
		String actorUserId,
		@Size(max = 200) String title,
		@Size(max = 2000) String description,
		@Size(max = 100) String category,
		@Min(1) @Max(5) Integer priority,
		@Future LocalDateTime deadline,
		@Positive @DecimalMax("1000") Double estimatedDurationHours,
		@Positive @DecimalMax("1000") Double correctedDurationHours,
		@Min(1) @Max(5) Integer complexity,
		TaskStatus status) {
}
