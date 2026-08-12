package com.srikrishnanethi.agamotto.dto.response;

import com.srikrishnanethi.agamotto.entities.enums.TaskStatus;

import java.time.Instant;
import java.time.LocalDateTime;

public record TaskResponse(
		String id,
		String projectId,
		String title,
		String description,
		String category,
		int priority,
		LocalDateTime deadline,
		double estimatedDurationHours,
		Double correctedDurationHours,
		int complexity,
		TaskStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
