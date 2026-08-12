package com.srikrishnanethi.agamotto.service;

import com.srikrishnanethi.agamotto.entities.Notification;

import java.util.List;

public interface NotificationService {

	List<Notification> listUnread(String userId);

	Notification markRead(String notificationId);
}
