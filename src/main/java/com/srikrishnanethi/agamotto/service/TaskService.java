package com.srikrishnanethi.agamotto.service;

import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.TaskHistory;
import com.srikrishnanethi.agamotto.entities.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskService {
    Task create(
            String projectId,
            String actorUserId,
            String title,
            String description,
            String category,
            int priority,
            LocalDateTime deadline,
            double estimatedDurationHours,
            int complexity);

    Task getById(String taskId);

    List<Task> listByProject(String projectId);

    Task update(
            String taskId,
            String actorUserId,
            String title,
            String description,
            String category,
            Integer priority,
            LocalDateTime deadline,
            Double estimatedDurationHours,
            Double correctedDurationHours,
            Integer complexity,
            TaskStatus status);

    /**
     * Soft-deletes the task (status {@link TaskStatus#CANCELLED}) and records a DELETED history row.
     */
    Task delete(String taskId, String userId);

    List<TaskHistory> historyForTask(String taskId);
}
