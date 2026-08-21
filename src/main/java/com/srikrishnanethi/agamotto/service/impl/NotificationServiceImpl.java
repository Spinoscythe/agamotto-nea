package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Notification;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.exception.ForbiddenException;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.NotificationRepository;
import com.srikrishnanethi.agamotto.service.NotificationService;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class NotificationServiceImpl implements NotificationService {

	private static final int MAX_MESSAGE = 500;

	private final NotificationRepository notificationRepository;

	public NotificationServiceImpl(NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notification> listUnread(String userId) {
		Objects.requireNonNull(userId, "userId");
		List<Notification> notifications =
				notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
		notifications.forEach(NotificationServiceImpl::initializeAssociations);
		return notifications;
	}

	@Override
	@Transactional
	public Notification markRead(String notificationId, String userId) {
		Objects.requireNonNull(notificationId, "notificationId");
		Objects.requireNonNull(userId, "userId");
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
		initializeAssociations(notification);
		if (notification.getUser() == null || !userId.equals(notification.getUser().getId())) {
			throw new ForbiddenException("You cannot access this notification");
		}
		notification.setRead(true);
		Notification saved = notificationRepository.save(notification);
		initializeAssociations(saved);
		return saved;
	}

	@Override
	@Transactional
	public Notification notifyUser(User user, Task task, String message) {
		Objects.requireNonNull(user, "user");
		Notification notification = new Notification();
		notification.setUser(user);
		notification.setTask(task);
		notification.setMessage(clampMessage(message));
		notification.setRead(false);
		notification.setCreatedAt(Instant.now());
		Notification saved = notificationRepository.save(notification);
		initializeAssociations(saved);
		return saved;
	}

	private static String clampMessage(String message) {
		String value = message == null || message.isBlank() ? "Notification" : message.trim();
		return value.length() <= MAX_MESSAGE ? value : value.substring(0, MAX_MESSAGE - 3) + "...";
	}

	private static void initializeAssociations(Notification notification) {
		if (notification.getUser() != null) {
			Hibernate.initialize(notification.getUser());
		}
		if (notification.getTask() != null) {
			Hibernate.initialize(notification.getTask());
		}
	}
}
