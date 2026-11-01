package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CRUD and graph queries for {@code task_dependencies}.
 */
@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, TaskDependency.Key> {

    /**
     * All prerequisite links for a task.
     * Used when checking if a task's dependencies are satisfied.
     */
    List<TaskDependency> findByTask(Task task);

    /**
     * All dependent links for a task.
     * Used when a task is removed or completed — we need to know what unlocks.
     */
    List<TaskDependency> findByDependsOnTask(Task task);

    /**
     * All dependency edges inside one project.
     * The scheduler loads the whole project graph at once.
     */
    @Query("select d from TaskDependency d join d.task t where t.project.id = :projectId")
    List<TaskDependency> findByProjectId(@Param("projectId") String projectId);

    /**
     * Delete all dependency edges for a task (both directions).
     * Called when a task is deleted.
     */
    void deleteByTask(Task task);

    void deleteByDependsOnTask(Task task);

    /**
     * True if adding this edge would create a cycle.
     * The service layer does the real DFS check, but this exists
     * for a quick DB-level guard if needed.
     */
    boolean existsByTaskAndDependsOnTask(Task task, Task dependsOnTask);
}