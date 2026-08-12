package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, String> {

	List<Task> findByProjectId(String projectId);

	List<Task> findByProjectIdAndStatus(String projectId, TaskStatus status);

	List<Task> findByProjectIdAndStatusIn(String projectId, Collection<TaskStatus> statuses);

	List<Task> findByProjectIdAndDeadlineBefore(String projectId, LocalDateTime deadline);

	/**
	 * Tasks whose deadline falls in {@code [from, to]} (inclusive) with an eligible status.
	 * Used by the daily deadline-reminder job (within the next 24 hours).
	 */
	List<Task> findByDeadlineBetweenAndStatusIn(
			LocalDateTime from,
			LocalDateTime to,
			Collection<TaskStatus> statuses);

	List<Task> findByProjectOwnerIdAndStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
			String ownerId,
			TaskStatus status,
			Instant updatedFrom,
			Instant updatedTo);
}
