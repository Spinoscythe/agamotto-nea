package com.srikrishnanethi.agamotto.dto.response;

import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;

import java.time.LocalDateTime;

public record ScheduleBlockResponse(
		String id,
		String scheduleId,
		String taskId,
		LocalDateTime startTime,
		LocalDateTime endTime,
		BlockDecision decision,
		String reason,
		boolean manuallyOverridden) {
}
