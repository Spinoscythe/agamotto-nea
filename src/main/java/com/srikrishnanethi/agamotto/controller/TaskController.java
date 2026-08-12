package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.request.CreateTaskRequest;
import com.srikrishnanethi.agamotto.dto.request.UpdateTaskRequest;
import com.srikrishnanethi.agamotto.dto.response.TaskHistoryResponse;
import com.srikrishnanethi.agamotto.dto.response.TaskResponse;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.mapper.TaskMapper;
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

    public TaskController(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(
            @PathVariable String projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        return taskMapper.toResponse(taskService.create(
                projectId,
                request.actorUserId(),
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
        return taskService.listByProject(projectId).stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @GetMapping({"/projects/{projectId}/tasks/{taskId}", "/tasks/{taskId}"})
    public TaskResponse get(
            @PathVariable(required = false) String projectId,
            @PathVariable String taskId) {
        return taskMapper.toResponse(requireTaskInProject(projectId, taskId));
    }

    @PatchMapping("/projects/{projectId}/tasks/{taskId}")
    public TaskResponse patchNested(
            @PathVariable String projectId,
            @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        requireTaskInProject(projectId, taskId);
        return applyUpdate(taskId, request);
    }

    @RequestMapping(path = "/tasks/{taskId}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public TaskResponse updateFlat(
            @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return applyUpdate(taskId, request);
    }

    @DeleteMapping("/projects/{projectId}/tasks/{taskId}")
    public TaskResponse deleteNested(
            @PathVariable String projectId,
            @PathVariable String taskId,
            @RequestParam String actorUserId) {
        requireTaskInProject(projectId, taskId);
        return taskMapper.toResponse(taskService.delete(taskId, actorUserId));
    }

    @DeleteMapping("/tasks/{taskId}")
    public TaskResponse deleteFlat(
            @PathVariable String taskId,
            @RequestParam String actorUserId) {
        return taskMapper.toResponse(taskService.delete(taskId, actorUserId));
    }

    @GetMapping({"/projects/{projectId}/tasks/{taskId}/history", "/tasks/{taskId}/history"})
    public List<TaskHistoryResponse> history(
            @PathVariable(required = false) String projectId,
            @PathVariable String taskId) {
        requireTaskInProject(projectId, taskId);
        return taskService.historyForTask(taskId).stream()
                .map(taskMapper::toHistoryResponse)
                .toList();
    }

    private TaskResponse applyUpdate(String taskId, UpdateTaskRequest request) {
        return taskMapper.toResponse(taskService.update(
                taskId,
                request.actorUserId(),
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

    private Task requireTaskInProject(String projectId, String taskId) {
        var task = taskService.getById(taskId);
        if (projectId != null && !projectId.equals(task.getProject().getId())) {
            throw new com.srikrishnanethi.agamotto.exception.ResourceNotFoundException(
                    "Task not found in project: " + taskId);
        }
        return task;
    }
}
