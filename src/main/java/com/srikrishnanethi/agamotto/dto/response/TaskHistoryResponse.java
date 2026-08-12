package com.srikrishnanethi.agamotto.dto.response;

import com.srikrishnanethi.agamotto.entities.enums.ChangeType;

import java.time.Instant;

public record TaskHistoryResponse(
		String id,
		String taskId,
		String changedByUserId,
		ChangeType changeType,
		String changeSummary,
		Instant changedAt) {
}
