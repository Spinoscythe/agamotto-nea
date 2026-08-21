package com.srikrishnanethi.agamotto.service;

import com.srikrishnanethi.agamotto.entities.Notification;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.User;

import java.util.List;

public interface NotificationService {

	List<Notification> listUnread(String userId);

	Notification markRead(String notificationId, String userId);

	Notification notifyUser(User user, Task task, String message);
}
