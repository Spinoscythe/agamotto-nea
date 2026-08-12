package com.srikrishnanethi.agamotto.dto.response;

import java.time.Instant;

public record NotificationResponse(
		String id,
		String userId,
		String taskId,
		String message,
		Instant createdAt,
		Instant sentAt,
		boolean read) {
}
