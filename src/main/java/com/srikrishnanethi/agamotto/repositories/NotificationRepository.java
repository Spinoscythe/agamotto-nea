package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Inbox queries for {@code notifications}. Recipients never see other users'
 * rows because list finders include {@code userId}.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    /**
     * Unread inbox for one user, newest first. Powers {@code GET /api/notifications}.
     * {@code read} maps to column {@code is_read}.
     */
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);

    /**
     * Alerts that point at a given invite. When an invite is cancelled or
     * resolved, these rows are marked read so the badge drops.
     */
    List<Notification> findByInviteId(String inviteId);

    /**
     * Delete every alert tied to a project. Called before the project row
     * itself is deleted so the FK does not block the drop.
     */
    void deleteByProjectId(String projectId);
}
