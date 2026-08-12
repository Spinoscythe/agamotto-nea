package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.ProjectMember;
import com.srikrishnanethi.agamotto.entities.enums.ProjectRole;
import com.srikrishnanethi.agamotto.exception.ForbiddenException;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.ProjectMemberRepository;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import com.srikrishnanethi.agamotto.service.ProjectAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class ProjectAccessServiceImpl implements ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectAccessServiceImpl(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectRole> findRole(String userId, String projectId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(projectId, "projectId");

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return Optional.empty();
        }

        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(ProjectMember::getRole)
                .or(() -> {
                    if (project.getOwner() != null && userId.equals(project.getOwner().getId())) {
                        return Optional.of(ProjectRole.OWNER);
                    }
                    return Optional.empty();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectRole requireView(String userId, String projectId) {
        return requireRole(userId, projectId, true, false, false);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectRole requireEdit(String userId, String projectId) {
        return requireRole(userId, projectId, true, true, false);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectRole requireOwner(String userId, String projectId) {
        return requireRole(userId, projectId, true, true, true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccess(String userId, String projectId) {
        return findRole(userId, projectId).isPresent();
    }

    private ProjectRole requireRole(
            String userId,
            String projectId,
            boolean view,
            boolean edit,
            boolean owner) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }
        ProjectRole role = findRole(userId, projectId)
                .orElseThrow(() -> new ForbiddenException("Not a member of project: " + projectId));
        if (owner && !role.canManageMembers()) {
            throw new ForbiddenException("Owner role required for project: " + projectId);
        }
        if (edit && !role.canEdit()) {
            throw new ForbiddenException("Edit role required for project: " + projectId);
        }
        if (view && !role.canView()) {
            throw new ForbiddenException("View role required for project: " + projectId);
        }
        return role;
    }
}
