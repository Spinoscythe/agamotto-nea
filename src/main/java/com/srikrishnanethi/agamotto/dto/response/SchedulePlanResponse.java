package com.srikrishnanethi.agamotto.dto.response;

import com.srikrishnanethi.agamotto.entities.enums.PlanStatus;
import com.srikrishnanethi.agamotto.entities.enums.ScheduleMode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SchedulePlanResponse(
        String id,
        String projectId,
        ScheduleMode mode,
        PlanStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Instant generatedAt,
        String explanationSummary,
        List<ScheduleBlockResponse> blocks) {
}
