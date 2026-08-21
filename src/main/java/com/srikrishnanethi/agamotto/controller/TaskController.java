package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.request.CreateTaskRequest;
import com.srikrishnanethi.agamotto.dto.request.UpdateTaskRequest;
import com.srikrishnanethi.agamotto.dto.response.TaskHistoryResponse;
import com.srikrishnanethi.agamotto.dto.response.TaskResponse;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.mapper.TaskMapper;
import com.srikrishnanethi.agamotto.security.AgamottoSecurity;
import com.srikrishnanethi.agamotto.service.ProjectService;
import com.srikrishnanethi.agamotto.service.TaskService;
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
    private final ProjectService projectService;

    public TaskController(TaskService taskService, TaskMapper taskMapper, ProjectService projectService) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
        this.projectService = projectService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(
            @PathVariable String projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        String actorId = AgamottoSecurity.currentUserId();
        AgamottoSecurity.requireSelf(request.actorUserId());
        AgamottoSecurity.requireOwner(projectService.getById(projectId));
        return taskMapper.toResponse(taskService.create(
                projectId,
                actorId,
                request.title(),
                request.description(),
                request.category(),
                request.priority(),
                request.deadline(),
                request.estimatedDurationHours(),
                request.complexity()));
    }

    @GetMapping("/projects/{projectId}/tasks")
    public List<TaskResponse> listByProject(@PathVariable String projectId) {
        AgamottoSecurity.requireOwner(projectService.getById(projectId));
        return taskService.listByProject(projectId).stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @GetMapping({"/projects/{projectId}/tasks/{taskId}", "/tasks/{taskId}"})
    public TaskResponse get(
            @PathVariable(required = false) String projectId,
            @PathVariable String taskId) {
        return taskMapper.toResponse(requireOwnedTask(projectId, taskId));
    }

    @PatchMapping("/projects/{projectId}/tasks/{taskId}")
    public TaskResponse patchNested(
            @PathVariable String projectId,
            @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        requireOwnedTask(projectId, taskId);
        return applyUpdate(taskId, request);
    }

    @RequestMapping(path = "/tasks/{taskId}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public TaskResponse updateFlat(
            @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        requireOwnedTask(null, taskId);
        return applyUpdate(taskId, request);
    }

    @DeleteMapping("/projects/{projectId}/tasks/{taskId}")
    public TaskResponse deleteNested(
            @PathVariable String projectId,
            @PathVariable String taskId,
            @RequestParam(required = false) String actorUserId) {
        requireOwnedTask(projectId, taskId);
        AgamottoSecurity.requireSelf(actorUserId);
        return taskMapper.toResponse(taskService.delete(taskId, AgamottoSecurity.currentUserId()));
    }

    @DeleteMapping("/tasks/{taskId}")
    public TaskResponse deleteFlat(
            @PathVariable String taskId,
            @RequestParam(required = false) String actorUserId) {
        requireOwnedTask(null, taskId);
        AgamottoSecurity.requireSelf(actorUserId);
        return taskMapper.toResponse(taskService.delete(taskId, AgamottoSecurity.currentUserId()));
    }

    @GetMapping({"/projects/{projectId}/tasks/{taskId}/history", "/tasks/{taskId}/history"})
    public List<TaskHistoryResponse> history(
            @PathVariable(required = false) String projectId,
            @PathVariable String taskId) {
        requireOwnedTask(projectId, taskId);
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

    private Task requireOwnedTask(String projectId, String taskId) {
        Task task = taskService.getById(taskId);
        if (projectId != null && !projectId.equals(task.getProject().getId())) {
            throw new com.srikrishnanethi.agamotto.exception.ResourceNotFoundException(
                    "Task not found in project: " + taskId);
        }
        AgamottoSecurity.requireOwner(task);
        return task;
    }
}
