package com.srikrishnanethi.agamotto.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProjectRequest(
		@Size(max = 200) String name,
		@Size(max = 2000) String description,
		LocalDate startDate,
		LocalDate endDate,
		@PositiveOrZero Double estimatedEffortHours) {
}
