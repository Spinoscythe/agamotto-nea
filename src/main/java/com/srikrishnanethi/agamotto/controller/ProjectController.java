package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.request.CreateProjectRequest;
import com.srikrishnanethi.agamotto.dto.request.UpdateProjectRequest;
import com.srikrishnanethi.agamotto.dto.response.ProjectResponse;
import com.srikrishnanethi.agamotto.mapper.ProjectMapper;
import com.srikrishnanethi.agamotto.security.AgamottoSecurity;
import com.srikrishnanethi.agamotto.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;

    public ProjectController(ProjectService projectService, ProjectMapper projectMapper) {
        this.projectService = projectService;
        this.projectMapper = projectMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) {
        String ownerId = AgamottoSecurity.currentUserId();
        AgamottoSecurity.requireSelf(request.ownerId());
        return this.projectMapper.toResponse(this.projectService.create(ownerId,
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate(),
                request.estimatedEffortHours()));
    }

    @GetMapping
    public List<ProjectResponse> list(@RequestParam(required = false) String ownerId) {
        String me = AgamottoSecurity.currentUserId();
        AgamottoSecurity.requireSelf(ownerId);
        return this.projectService.listByOwner(me).stream().map(projectMapper::toResponse).toList();
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@PathVariable String projectId) {
        var project = this.projectService.getById(projectId);
        AgamottoSecurity.requireOwner(project);
        return this.projectMapper.toResponse(project);
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(@PathVariable String projectId, @Valid @RequestBody UpdateProjectRequest request) {
        AgamottoSecurity.requireOwner(this.projectService.getById(projectId));
        return this.projectMapper.toResponse(this.projectService.update(projectId,
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate(),
                request.estimatedEffortHours()));
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String projectId) {
        AgamottoSecurity.requireOwner(this.projectService.getById(projectId));
        this.projectService.delete(projectId);
    }
}
