package com.srikrishnanethi.agamotto.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record UpdateUserRequest(
		@Size(max = 120) @JsonAlias("displayName") String fullName,
		@JsonAlias("workingDayStart") LocalTime preferredStart,
		@JsonAlias("workingDayEnd") LocalTime preferredEnd,
		Boolean includeWeekends,
		Double weightPriority,
		Double weightUrgency,
		Double weightDuration) {
}
