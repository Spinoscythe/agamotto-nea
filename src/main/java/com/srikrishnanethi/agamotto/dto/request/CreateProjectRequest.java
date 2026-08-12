package com.srikrishnanethi.agamotto.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateProjectRequest(
		@NotBlank String ownerId,
		@NotBlank @Size(max = 200) String name,
		@Size(max = 2000) String description,
		@NotNull LocalDate startDate,
		@NotNull LocalDate endDate,
		@PositiveOrZero double estimatedEffortHours) {
}
