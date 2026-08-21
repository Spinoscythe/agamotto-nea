package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.ProjectMember;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.enums.ProjectRole;
import com.srikrishnanethi.agamotto.exception.ConflictException;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.NotificationRepository;
import com.srikrishnanethi.agamotto.repositories.ProjectInviteRepository;
import com.srikrishnanethi.agamotto.repositories.ProjectMemberRepository;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import com.srikrishnanethi.agamotto.repositories.SchedulePlanRepository;
import com.srikrishnanethi.agamotto.repositories.TaskRepository;
import com.srikrishnanethi.agamotto.repositories.UserRepository;
import com.srikrishnanethi.agamotto.service.ProjectService;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final SchedulePlanRepository schedulePlanRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInviteRepository projectInviteRepository;
    private final NotificationRepository notificationRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository,
                              UserRepository userRepository,
                              TaskRepository taskRepository,
                              SchedulePlanRepository schedulePlanRepository,
                              ProjectMemberRepository projectMemberRepository,
                              ProjectInviteRepository projectInviteRepository,
                              NotificationRepository notificationRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.schedulePlanRepository = schedulePlanRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectInviteRepository = projectInviteRepository;
        this.notificationRepository = notificationRepository;
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
        ProjectMember ownerMembership = new ProjectMember();
        ownerMembership.setProject(save);
        ownerMembership.setUser(user);
        ownerMembership.setRole(ProjectRole.OWNER);
        ownerMembership.setJoinedAt(Instant.now());
        this.projectMemberRepository.save(ownerMembership);
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
    public List<Project> listAccessible(String userId) {
        Map<String, Project> unique = new LinkedHashMap<>();
        for (Project project : this.projectRepository.findByOwnerId(userId)) {
            unique.put(project.getId(), project);
        }
        for (ProjectMember member : this.projectMemberRepository.findByUserId(userId)) {
            Project project = member.getProject();
            if (project != null && project.getId() != null) {
                unique.putIfAbsent(project.getId(), project);
            }
        }
        List<Project> projects = List.copyOf(unique.values());
        projects.forEach(ProjectServiceImpl::initializeOwner);
        return projects;
    }

    @Override
    @Transactional
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
        if (schedulePlanRepository.existsByProjectId(projectId)) {
            throw new ConflictException("Cannot delete project with existing schedules: " + projectId);
        }
        notificationRepository.deleteByProjectId(projectId);
        projectInviteRepository.deleteByProjectId(projectId);
        projectMemberRepository.deleteByProjectId(projectId);
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
