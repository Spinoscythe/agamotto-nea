package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.TaskHistory;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.enums.ChangeType;
import com.srikrishnanethi.agamotto.repositories.TaskHistoryRepository;

import java.time.Instant;

final class TaskHistoryRecorder {

    private static final int MAX_SUMMARY_LENGTH = 1000;

    private final TaskHistoryRepository taskHistoryRepository;

    TaskHistoryRecorder(TaskHistoryRepository taskHistoryRepository) {
        this.taskHistoryRepository = taskHistoryRepository;
    }

    void record(Task task, User actor, ChangeType type, String summary) {
        TaskHistory history = new TaskHistory();
        history.setTask(task);
        history.setChangedBy(actor);
        history.setChangeType(type);
        history.setChangeSummary(truncate(summary));
        history.setChangedAt(Instant.now());
        taskHistoryRepository.save(history);
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_SUMMARY_LENGTH
                ? value
                : value.substring(0, MAX_SUMMARY_LENGTH - 3) + "...";
    }
}
