package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Remove the audit trail for every task in a project. Called before the
     * project's tasks are deleted so the {@code tasks} FK does not block it.
     */
    @Modifying
    @Query("""
            delete from TaskHistory h
            where h.task.id in (
                select t.id from Task t where t.project.id = :projectId
            )
            """)
    void deleteByProjectId(@Param("projectId") String projectId);
}
