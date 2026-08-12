package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.exception.ConflictException;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import com.srikrishnanethi.agamotto.repositories.TaskRepository;
import com.srikrishnanethi.agamotto.repositories.UserRepository;
import com.srikrishnanethi.agamotto.service.ProjectService;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository, UserRepository userRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional
    public Project create(String ownerId,
                          String name,
                          String description,
                          LocalDate startDate,
                          LocalDate endDate,
                          double estimatedEffortHours) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");

        User user = this.userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + ownerId));
        validateDateRange(startDate, endDate);
        Project project = new Project();
        project.setOwner(user);
        project.setCreatedAt(Instant.now());
        project.setName(name);
        project.setDescription(description);
        project.setStartDate(startDate);
        project.setEndDate(endDate);
        project.setEstimatedEffortHours(estimatedEffortHours);
        Project save = this.projectRepository.save(project);
        initializeOwner(save);
        return save;
    }

    @Override
    @Transactional(readOnly = true)
    public Project getById(String projectId) {
        Project project = this.projectRepository.findById(projectId).orElseThrow(() ->
                new ResourceNotFoundException("Project not found: " + projectId));
        initializeOwner(project);
        return project;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> listByOwner(String ownerId) {
        List<Project> projects = this.projectRepository.findByOwnerId(ownerId);
        projects.forEach(ProjectServiceImpl::initializeOwner);
        return projects;
    }

    @Override
    public Project update(String projectId,
                          String name,
                          String description,
                          LocalDate startDate,
                          LocalDate endDate,
                          Double estimatedEffortHours) {
        Project project = getById(projectId);

        if (name != null && !name.isBlank()) {
            project.setName(name.trim());
        }
        if (description != null) {
            project.setDescription(description);
        }
        if (startDate != null) {
            project.setStartDate(startDate);
        }
        if (endDate != null) {
            project.setEndDate(endDate);
        }
        if (estimatedEffortHours != null) {
            project.setEstimatedEffortHours(estimatedEffortHours);
        }
        validateDateRange(project.getStartDate(), project.getEndDate());
        Project saved = projectRepository.save(project);
        initializeOwner(saved);
        return saved;
    }

    @Override
    @Transactional
    public void delete(String projectId) {
        Project project = getById(projectId);
        if (!taskRepository.findByProjectId(projectId).isEmpty()) {
            throw new ConflictException("Cannot delete project with existing tasks: " + projectId);
        }
        projectRepository.delete(project);
    }

    private static void initializeOwner(Project project) {
        if (project.getOwner() != null) {
            Hibernate.initialize(project.getOwner());
        }
    }

    private static void validateDateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
    }
}
