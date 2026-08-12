package com.srikrishnanethi.agamotto.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GenerateScheduleRequest(
		@NotNull LocalDate startDate,
		@NotNull LocalDate endDate) {
}
