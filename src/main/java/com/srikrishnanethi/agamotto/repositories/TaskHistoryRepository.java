package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Append-only reads of {@code task_history}. Writes go through {@code JpaRepository.save}.
 */
@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, String> {

    /**
     * Full audit trail for one task, newest first so the UI shows the latest
     * change at the top.
     */
    List<TaskHistory> findByTaskIdOrderByChangedAtDesc(String taskId);
}
