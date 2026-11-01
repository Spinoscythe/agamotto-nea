package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.enums.TaskStatus;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class TaskUpdateApplier {

    private final UserRepository userRepository;

    TaskUpdateApplier(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    UpdateResult apply(Task task,
                       String actorUserId,
                       String title,
                       String description,
                       String category,
                       Integer priority,
                       LocalDateTime deadline,
                       Double estimatedDurationHours,
                       Double correctedDurationHours,
                       Integer complexity,
                       TaskStatus status) {
        List<String> fieldChanges = new ArrayList<>();
        boolean statusChanged = false;
        TaskStatus previousStatus = task.getStatus();
        User user = requireUser(actorUserId);

        if (title != null && !title.isBlank() && !title.trim().equals(task.getTitle())) {
            fieldChanges.add("title: '" + task.getTitle() + "' -> '" + title.trim() + "'");
            task.setTitle(title.trim());
        }
        if (description != null && !Objects.equals(description, task.getDescription())) {
            fieldChanges.add("description updated");
            task.setDescription(description);
        }
        if (category != null && !category.isBlank() && !category.trim().equals(task.getCategory())) {
            fieldChanges.add("category: '" + task.getCategory() + "' -> '" + category.trim() + "'");
            task.setCategory(category.trim());
        }
        if (priority != null && priority != task.getPriority()) {
            fieldChanges.add("priority: " + task.getPriority() + " -> " + priority);
            task.setPriority(priority);
        }
        if (deadline != null && !deadline.equals(task.getDeadline())) {
            TaskDeadlineValidator.validateWithinProject(task.getProject(), deadline);
            fieldChanges.add("deadline: " + task.getDeadline() + " -> " + deadline);
            task.setDeadline(deadline);
        }
        if (estimatedDurationHours != null
                && Double.compare(estimatedDurationHours, task.getEstimatedDurationHours()) != 0) {
            fieldChanges.add("estimatedDurationHours: " + task.getEstimatedDurationHours()
                    + " -> " + estimatedDurationHours);
            task.setEstimatedDurationHours(estimatedDurationHours);
        }
        if (correctedDurationHours != null
                && !Objects.equals(correctedDurationHours, task.getCorrectedDurationHours())) {
            fieldChanges.add("correctedDurationHours: " + task.getCorrectedDurationHours()
                    + " -> " + correctedDurationHours);
            task.setCorrectedDurationHours(correctedDurationHours);
        }
        if (complexity != null && complexity != task.getComplexity()) {
            fieldChanges.add("complexity: " + task.getComplexity() + " -> " + complexity);
            task.setComplexity(complexity);
        }
        if (status != null && status != task.getStatus()) {
            statusChanged = true;
            task.setStatus(status);
        }

        return new UpdateResult(user, fieldChanges, statusChanged, previousStatus);
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    record UpdateResult(
            User actor,
            List<String> fieldChanges,
            boolean statusChanged,
            TaskStatus previousStatus) {
    }
}
