package com.srikrishnanethi.agamotto.mapper;

import com.srikrishnanethi.agamotto.dto.response.NotificationResponse;
import com.srikrishnanethi.agamotto.entities.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

	public NotificationResponse toResponse(Notification notification) {
		return new NotificationResponse(
				notification.getId(),
				notification.getUser().getId(),
				notification.getTask() != null ? notification.getTask().getId() : null,
				notification.getMessage(),
				notification.getCreatedAt(),
				notification.getSentAt(),
				notification.isRead());
	}
}
