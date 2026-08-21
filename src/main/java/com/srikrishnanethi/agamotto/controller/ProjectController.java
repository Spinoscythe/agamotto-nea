package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.request.CreateProjectRequest;
import com.srikrishnanethi.agamotto.dto.request.UpdateProjectRequest;
import com.srikrishnanethi.agamotto.dto.response.ProjectResponse;
import com.srikrishnanethi.agamotto.mapper.ProjectMapper;
import com.srikrishnanethi.agamotto.security.AgamottoSecurity;
import com.srikrishnanethi.agamotto.service.ProjectAccessService;
import com.srikrishnanethi.agamotto.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;
    private final ProjectAccessService projectAccessService;

    public ProjectController(
            ProjectService projectService,
            ProjectMapper projectMapper,
            ProjectAccessService projectAccessService) {
        this.projectService = projectService;
        this.projectMapper = projectMapper;
        this.projectAccessService = projectAccessService;
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
        return this.projectService.listAccessible(me).stream().map(projectMapper::toResponse).toList();
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@PathVariable String projectId) {
        projectAccessService.requireView(AgamottoSecurity.currentUserId(), projectId);
        return this.projectMapper.toResponse(this.projectService.getById(projectId));
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(@PathVariable String projectId, @Valid @RequestBody UpdateProjectRequest request) {
        projectAccessService.requireEdit(AgamottoSecurity.currentUserId(), projectId);
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
        projectAccessService.requireOwner(AgamottoSecurity.currentUserId(), projectId);
        this.projectService.delete(projectId);
    }
}
