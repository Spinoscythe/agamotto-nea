package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Notification;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.NotificationRepository;
import com.srikrishnanethi.agamotto.service.NotificationService;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class NotificationServiceImpl implements NotificationService {

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
	public Notification markRead(String notificationId) {
		Objects.requireNonNull(notificationId, "notificationId");
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
		notification.setRead(true);
		Notification saved = notificationRepository.save(notification);
		initializeAssociations(saved);
		return saved;
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
