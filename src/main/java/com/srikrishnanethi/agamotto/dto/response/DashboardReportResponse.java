package com.srikrishnanethi.agamotto.dto.response;

import com.srikrishnanethi.agamotto.entities.enums.ReportPeriod;

import java.time.Instant;
import java.time.LocalDate;

public record DashboardReportResponse(
		String id,
		String userId,
		ReportPeriod period,
		LocalDate periodStart,
		LocalDate periodEnd,
		int scheduledCount,
		int delayedCount,
		int excludedCount,
		int completedCount,
		Instant generatedAt) {
}
