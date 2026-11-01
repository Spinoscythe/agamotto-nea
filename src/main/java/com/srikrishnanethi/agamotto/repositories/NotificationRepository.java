package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Delete every alert tied to a project. Called before the project row
     * itself is deleted so the FK does not block the drop.
     */
    @Modifying
    @Query("delete from Notification n where n.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") String projectId);

    /**
     * Delete every alert that references a task in the project, even when the
     * alert's {@code project_id} was not populated. Must run before the tasks
     * are deleted so the {@code notifications.task_id} FK does not block it.
     */
    @Modifying
    @Query("""
            delete from Notification n
            where n.task.id in (
                select t.id from Task t where t.project.id = :projectId
            )
            """)
    void deleteByTaskProjectId(@Param("projectId") String projectId);
}
