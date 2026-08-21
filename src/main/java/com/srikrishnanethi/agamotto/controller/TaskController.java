package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.request.CreateTaskRequest;
import com.srikrishnanethi.agamotto.dto.request.UpdateTaskRequest;
import com.srikrishnanethi.agamotto.dto.response.TaskHistoryResponse;
import com.srikrishnanethi.agamotto.dto.response.TaskResponse;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.mapper.TaskMapper;
import com.srikrishnanethi.agamotto.security.AgamottoSecurity;
import com.srikrishnanethi.agamotto.service.ProjectAccessService;
import com.srikrishnanethi.agamotto.service.TaskService;
import com.srikrishnanethi.agamotto.service.realtime.ProjectEventPublisher;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Task CRUD nested under projects (NEA R2, T4/T5/T14).
 * Flat {@code /api/tasks/{id}} routes are kept as aliases for existing clients/tests.
 */
@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;
    private final ProjectAccessService projectAccessService;
    private final ProjectEventPublisher projectEventPublisher;

    public TaskController(
            TaskService taskService,
            TaskMapper taskMapper,
            ProjectAccessService projectAccessService,
            ProjectEventPublisher projectEventPublisher) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
        this.projectAccessService = projectAccessService;
        this.projectEventPublisher = projectEventPublisher;
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(
            @PathVariable String projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        String actorId = AgamottoSecurity.currentUserId();
        AgamottoSecurity.requireSelf(request.actorUserId());
        projectAccessService.requireEdit(actorId, projectId);
        TaskResponse created = taskMapper.toResponse(taskService.create(
                projectId,
                actorId,
                request.title(),
                request.description(),
                request.category(),
                request.priority(),
                request.deadline(),
                request.estimatedDurationHours(),
                request.complexity()));
        projectEventPublisher.publishTaskChanged(projectId, actorId);
        return created;
    }

    @GetMapping("/projects/{projectId}/tasks")
    public List<TaskResponse> listByProject(@PathVariable String projectId) {
        projectAccessService.requireView(AgamottoSecurity.currentUserId(), projectId);
        return taskService.listByProject(projectId).stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @GetMapping({"/projects/{projectId}/tasks/{taskId}", "/tasks/{taskId}"})
    public TaskResponse get(
            @PathVariable(required = false) String projectId,
            @PathVariable String taskId) {
        return taskMapper.toResponse(requireAccessibleTask(projectId, taskId, false));
    }

    @PatchMapping("/projects/{projectId}/tasks/{taskId}")
    public TaskResponse patchNested(
            @PathVariable String projectId,
            @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        Task task = requireAccessibleTask(projectId, taskId, true);
        TaskResponse updated = applyUpdate(taskId, request);
        projectEventPublisher.publishTaskChanged(task.getProject().getId(), AgamottoSecurity.currentUserId());
        return updated;
    }

    @RequestMapping(path = "/tasks/{taskId}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public TaskResponse updateFlat(
            @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        Task task = requireAccessibleTask(null, taskId, true);
        TaskResponse updated = applyUpdate(taskId, request);
        projectEventPublisher.publishTaskChanged(task.getProject().getId(), AgamottoSecurity.currentUserId());
        return updated;
    }

    @DeleteMapping("/projects/{projectId}/tasks/{taskId}")
    public TaskResponse deleteNested(
            @PathVariable String projectId,
            @PathVariable String taskId,
            @RequestParam(required = false) String actorUserId) {
        Task task = requireAccessibleTask(projectId, taskId, true);
        AgamottoSecurity.requireSelf(actorUserId);
        String actorId = AgamottoSecurity.currentUserId();
        TaskResponse deleted = taskMapper.toResponse(taskService.delete(taskId, actorId));
        projectEventPublisher.publishTaskChanged(task.getProject().getId(), actorId);
        return deleted;
    }

    @DeleteMapping("/tasks/{taskId}")
    public TaskResponse deleteFlat(
            @PathVariable String taskId,
            @RequestParam(required = false) String actorUserId) {
        Task task = requireAccessibleTask(null, taskId, true);
        AgamottoSecurity.requireSelf(actorUserId);
        String actorId = AgamottoSecurity.currentUserId();
        TaskResponse deleted = taskMapper.toResponse(taskService.delete(taskId, actorId));
        projectEventPublisher.publishTaskChanged(task.getProject().getId(), actorId);
        return deleted;
    }

    @GetMapping({"/projects/{projectId}/tasks/{taskId}/history", "/tasks/{taskId}/history"})
    public List<TaskHistoryResponse> history(
            @PathVariable(required = false) String projectId,
            @PathVariable String taskId) {
        requireAccessibleTask(projectId, taskId, false);
        return taskService.historyForTask(taskId).stream()
                .map(taskMapper::toHistoryResponse)
                .toList();
    }

    private TaskResponse applyUpdate(String taskId, UpdateTaskRequest request) {
        String actorId = AgamottoSecurity.currentUserId();
        AgamottoSecurity.requireSelf(request.actorUserId());
        return taskMapper.toResponse(taskService.update(
                taskId,
                actorId,
                request.title(),
                request.description(),
                request.category(),
                request.priority(),
                request.deadline(),
                request.estimatedDurationHours(),
                request.correctedDurationHours(),
                request.complexity(),
                request.status()));
    }

    private Task requireAccessibleTask(String projectId, String taskId, boolean edit) {
        Task task = taskService.getById(taskId);
        String resolvedProjectId = task.getProject().getId();
        if (projectId != null && !projectId.equals(resolvedProjectId)) {
            throw new com.srikrishnanethi.agamotto.exception.ResourceNotFoundException(
                    "Task not found in project: " + taskId);
        }
        String userId = AgamottoSecurity.currentUserId();
        if (edit) {
            projectAccessService.requireEdit(userId, resolvedProjectId);
        } else {
            projectAccessService.requireView(userId, resolvedProjectId);
        }
        return task;
    }
}
