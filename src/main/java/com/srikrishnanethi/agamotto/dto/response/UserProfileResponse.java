package com.srikrishnanethi.agamotto.dto.response;

import java.time.Instant;
import java.time.LocalTime;

public record UserProfileResponse(
		String id,
		LocalTime preferredStart,
		LocalTime preferredEnd,
		boolean includeWeekends,
		double weightPriority,
		double weightUrgency,
		double weightDuration,
		Instant updatedAt) {
}
