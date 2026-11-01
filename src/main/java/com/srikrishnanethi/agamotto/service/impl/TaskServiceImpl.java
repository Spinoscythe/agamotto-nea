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
import java.util.List;
import java.util.Objects;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final UserRepository userRepository;
    private final TaskUpdateApplier taskUpdateApplier;
    private final TaskHistoryRecorder taskHistoryRecorder;

    public TaskServiceImpl(TaskRepository taskRepository,
                           ProjectRepository projectRepository,
                           TaskHistoryRepository taskHistoryRepository,
                           UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.taskHistoryRepository = taskHistoryRepository;
        this.userRepository = userRepository;
        this.taskUpdateApplier = new TaskUpdateApplier(userRepository);
        this.taskHistoryRecorder = new TaskHistoryRecorder(taskHistoryRepository);
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
                       int complexity,
                       TaskStatus status) {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(deadline, "deadline");

        Project project = this.projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        TaskDeadlineValidator.validateWithinProject(project, deadline);

        Task task = new Task();
        task.setProject(project);
        task.setTitle(title.trim());
        task.setDescription(description);
        task.setCategory(category.trim());
        task.setPriority(priority);
        task.setDeadline(deadline);
        task.setEstimatedDurationHours(estimatedDurationHours);
        task.setComplexity(complexity);
        task.setStatus(status != null ? status : TaskStatus.PENDING);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());

        task = this.taskRepository.save(task);
        User actor = requireUser(userId);
        taskHistoryRecorder.record(task, actor, ChangeType.CREATED,
                "Created task '" + task.getTitle() + "'");

        initializeProject(task);
        return task;
    }

    @Override
    @Transactional(readOnly = true)
    public Task getById(String taskId) {
        Task task = this.taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        initializeProject(task);
        return task;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> listByProject(String projectId) {
        List<Task> tasks = this.taskRepository.findByProjectId(projectId);
        tasks.forEach(TaskServiceImpl::initializeProject);
        return tasks;
    }

    @Override
    @Transactional
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
        TaskUpdateApplier.UpdateResult result = taskUpdateApplier.apply(
                task, actorUserId, title, description, category, priority, deadline,
                estimatedDurationHours, correctedDurationHours, complexity, status);

        task.setUpdatedAt(Instant.now());
        task = taskRepository.save(task);

        if (result.statusChanged()) {
            taskHistoryRecorder.record(task, result.actor(), ChangeType.STATUS_CHANGED,
                    "status: " + result.previousStatus() + " -> " + task.getStatus()
                            + (result.fieldChanges().isEmpty() ? "" : "; " + String.join("; ", result.fieldChanges())));
        } else {
            String summary = result.fieldChanges().isEmpty()
                    ? "Updated task '" + task.getTitle() + "' (no field changes)"
                    : "Updated task '" + task.getTitle() + "': " + String.join("; ", result.fieldChanges());
            taskHistoryRecorder.record(task, result.actor(), ChangeType.EDITED, summary);
        }
        initializeProject(task);
        return task;
    }

    @Override
    @Transactional
    public Task delete(String taskId, String userId) {
        Task task = this.getById(taskId);
        User user = requireUser(userId);

        task.setStatus(TaskStatus.CANCELLED);
        task.setUpdatedAt(Instant.now());
        task = this.taskRepository.save(task);

        taskHistoryRecorder.record(task, user, ChangeType.DELETED,
                "Deleted (cancelled) task '" + task.getTitle() + "'");
        initializeProject(task);
        return task;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskHistory> historyForTask(String taskId) {
        List<TaskHistory> history = this.taskHistoryRepository.findByTaskIdOrderByChangedAtDesc(taskId);
        history.forEach(entry -> {
            Hibernate.initialize(entry.getTask());
            Hibernate.initialize(entry.getChangedBy());
        });
        return history;
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

}
