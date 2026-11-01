package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.exception.ForbiddenException;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import com.srikrishnanethi.agamotto.service.ProjectAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class ProjectAccessServiceImpl implements ProjectAccessService {

    private final ProjectRepository projectRepository;

    public ProjectAccessServiceImpl(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void requireAccess(String userId, String projectId) {
        requireOwner(userId, projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireOwner(String userId, String projectId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(projectId, "projectId");

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        if (project.getOwner() == null || !userId.equals(project.getOwner().getId())) {
            throw new ForbiddenException("You cannot access this project");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccess(String userId, String projectId) {
        if (userId == null || projectId == null) {
            return false;
        }
        return projectRepository.findById(projectId)
                .map(project -> project.getOwner() != null && userId.equals(project.getOwner().getId()))
                .orElse(false);
    }
}
