package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * CRUD and listing queries for {@code tasks}.
 *
 * Derived finders load graphs for the UI and the scheduler. Dashboard reports
 * reuse the status + updated-at window finders and count in memory (projects
 * are small enough that a dedicated COUNT query is unnecessary).
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    /**
     * All tasks in a project. Powers the Kanban / list views and the
     * "cannot delete a project that still has tasks" guard.
     */
    List<Task> findByProjectId(String projectId);

    /**
     * Incomplete (or otherwise eligible) tasks in a project. The scheduler
     * loads {@code PENDING} + {@code IN_PROGRESS} so it does not place
     * already-done or cancelled work.
     */
    List<Task> findByProjectIdAndStatusIn(String projectId, Collection<TaskStatus> statuses);

    /**
     * Owner's tasks that reached {@code status} with {@code updated_at} in
     * {@code [updatedFrom, updatedTo)}. Dashboard "completed this period" for
     * the signed-in user across every project they own.
     */
    List<Task> findByProjectOwnerIdAndStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
            String ownerId,
            TaskStatus status,
            Instant updatedFrom,
            Instant updatedTo);

    /**
     * Same window as {@link #findByProjectOwnerIdAndStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan}
     * but scoped to one project. Project-level dashboard reports.
     */
    List<Task> findByProjectIdAndStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
            String projectId,
            TaskStatus status,
            Instant updatedFrom,
            Instant updatedTo);
}
