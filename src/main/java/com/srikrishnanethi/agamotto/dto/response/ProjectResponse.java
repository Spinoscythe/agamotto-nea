package com.srikrishnanethi.agamotto.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record ProjectResponse(
		String id,
		String ownerId,
		String name,
		String description,
		LocalDate startDate,
		LocalDate endDate,
		double estimatedEffortHours,
		Instant createdAt) {
}
