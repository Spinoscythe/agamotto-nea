package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, String> {

    @Query(value = """
            SELECT *
            FROM task_history
            JOIN task ON task_history.task_id = task_id
            JOIN users ON task_history.changed_by = users.id
            WHERE task.id = :taskId
            ORDER BY task_history.changed_at DESC
            """, nativeQuery = true)
    List<TaskHistory> findByTaskIdOrderByChangedAtDesc(@Param("taskId") String taskId);
}
