package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Notification;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.exception.ForbiddenException;
import com.srikrishnanethi.agamotto.repositories.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

	@Mock
	private NotificationRepository notificationRepository;

	private NotificationServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new NotificationServiceImpl(notificationRepository);
	}

	@Test
	void markReadRejectsOtherUsersNotification() {
		User owner = new User();
		owner.setId("u1");
		Notification notification = new Notification();
		notification.setUser(owner);
		when(notificationRepository.findById("n1")).thenReturn(Optional.of(notification));

		assertThrows(ForbiddenException.class, () -> service.markRead("n1", "u2"));
		verify(notificationRepository, never()).save(any());
	}

	@Test
	void notifyUserClampsLongMessage() {
		User user = new User();
		user.setId("u1");
		when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

		Notification saved = service.notifyUser(user, null, "x".repeat(600));

		assertTrue(saved.getMessage().length() <= 500);
		assertTrue(saved.getMessage().endsWith("..."));
	}
}
