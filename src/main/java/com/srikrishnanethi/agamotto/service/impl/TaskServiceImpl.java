package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.TaskHistory;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.enums.ChangeType;
import com.srikrishnanethi.agamotto.entities.enums.TaskStatus;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import com.srikrishnanethi.agamotto.repositories.TaskHistoryRepository;
import com.srikrishnanethi.agamotto.repositories.TaskRepository;
import com.srikrishnanethi.agamotto.repositories.UserRepository;
import com.srikrishnanethi.agamotto.service.TaskService;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final UserRepository userRepository;

    public TaskServiceImpl(TaskRepository taskRepository,
                           ProjectRepository projectRepository,
                           TaskHistoryRepository taskHistoryRepository,
                           UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.taskHistoryRepository = taskHistoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Task create(String projectId,
                       String userId,
                       String title,
                       String description,
                       String category,
                       int priority,
                       LocalDateTime deadline,
                       double estimatedDurationHours,
                       int complexity) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(deadline, "deadline");

        Project project = this.projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        Task task = new Task();
        task.setProject(project);
        task.setTitle(title.trim());
        task.setDescription(description);
        task.setCategory(category.trim());
        task.setPriority(priority);
        task.setDeadline(deadline);
        task.setEstimatedDurationHours(estimatedDurationHours);
        task.setComplexity(complexity);
        task.setStatus(TaskStatus.PENDING);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        task = this.taskRepository.save(task);

        initializeProject(task);
        return task;
    }

    @Override
    public Task getById(String taskId) {
        Task task = this.taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        initializeProject(task);
        return task;
    }

    @Override
    public List<Task> listByProject(String projectId) {
        List<Task> tasks = this.taskRepository.findByProjectId(projectId);
        tasks.forEach(TaskServiceImpl::initializeProject);
        return tasks;
    }

    @Override
    public Task update(String taskId,
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
        Task task = this.getById(taskId);
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

        task.setUpdatedAt(Instant.now());
        task = taskRepository.save(task);

        if (statusChanged) {
            writeHistory(task, user, ChangeType.STATUS_CHANGED,
                    truncate("status: " + previousStatus + " -> " + task.getStatus()
                            + (fieldChanges.isEmpty() ? "" : "; " + String.join("; ", fieldChanges))));
        } else {
            String summary = fieldChanges.isEmpty()
                    ? "Updated task '" + task.getTitle() + "' (no field changes)"
                    : "Updated task '" + task.getTitle() + "': " + String.join("; ", fieldChanges);
            writeHistory(task, user, ChangeType.EDITED, truncate(summary));
        }
        initializeProject(task);
        return task;
    }

    @Override
    public Task delete(String taskId, String userId) {
        Task task = this.getById(taskId);
        User user = requireUser(userId);

        task.setStatus(TaskStatus.CANCELLED);
        task.setUpdatedAt(Instant.now());
        task = this.taskRepository.save(task);

        writeHistory(task, user, ChangeType.DELETED,
                "Deleted (cancelled) task '" + task.getTitle() + "'");
        initializeProject(task);
        return task;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskHistory> historyForTask(String taskId) {
        return this.taskHistoryRepository.findByTaskIdOrderByChangedAtDesc(taskId);
    }

    private static void initializeProject(Task task) {
        if (task.getProject() != null) {
            Hibernate.initialize(task.getProject());
        }
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private void writeHistory(Task task, User actor, ChangeType type, String summary) {
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
        return value.length() <= 1000 ? value : value.substring(0, 997) + "...";
    }
}
