package com.srikrishnanethi.agamotto.dto.response;

import com.srikrishnanethi.agamotto.entities.enums.NotificationType;

import java.time.Instant;

public record NotificationResponse(
		String id,
		String userId,
		String taskId,
		String projectId,
		String inviteId,
		NotificationType type,
		String message,
		Instant createdAt,
		Instant sentAt,
		boolean read) {
}
