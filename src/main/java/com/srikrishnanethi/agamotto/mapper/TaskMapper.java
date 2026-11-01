package com.srikrishnanethi.agamotto.mapper;

import com.srikrishnanethi.agamotto.dto.response.TaskHistoryResponse;
import com.srikrishnanethi.agamotto.dto.response.TaskResponse;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.TaskHistory;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

	public TaskResponse toResponse(Task task) {
		return new TaskResponse(
				task.getId(),
				task.getProject().getId(),
				task.getTitle(),
				task.getDescription(),
				task.getCategory(),
				task.getPriority(),
				task.getDeadline(),
				task.getEstimatedDurationHours(),
				task.getCorrectedDurationHours(),
				task.getComplexity(),
				task.getStatus(),
				task.getCreatedAt(),
				task.getUpdatedAt());
	}

	public TaskHistoryResponse toHistoryResponse(TaskHistory history) {
		return new TaskHistoryResponse(
				history.getId(),
				history.getTask().getId(),
				history.getChangedBy().getId(),
				history.getChangeType(),
				history.getChangeSummary(),
				history.getChangedAt());
	}
}
