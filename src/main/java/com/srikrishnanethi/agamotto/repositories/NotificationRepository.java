package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

	List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);

	List<Notification> findByInviteId(String inviteId);
}
