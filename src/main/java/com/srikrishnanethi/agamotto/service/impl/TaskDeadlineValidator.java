package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;

import java.time.LocalDate;
import java.time.LocalDateTime;

final class TaskDeadlineValidator {

    private TaskDeadlineValidator() {
    }

    static void validateWithinProject(Project project, LocalDateTime deadline) {
        if (project == null) {
            return;
        }
        LocalDate deadlineDate = deadline.toLocalDate();
        LocalDate projectStart = project.getStartDate();
        LocalDate projectEnd = project.getEndDate();
        if (projectStart != null && deadlineDate.isBefore(projectStart)) {
            throw new IllegalArgumentException(
                    "Task deadline must be on or after the project start date (" + projectStart + ").");
        }
        if (projectEnd != null && deadlineDate.isAfter(projectEnd)) {
            throw new IllegalArgumentException(
                    "Task deadline must be on or before the project end date (" + projectEnd + ").");
        }
    }
}
