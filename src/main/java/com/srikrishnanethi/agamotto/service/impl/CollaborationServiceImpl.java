package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.*;
import com.srikrishnanethi.agamotto.entities.enums.InviteStatus;
import com.srikrishnanethi.agamotto.entities.enums.NotificationType;
import com.srikrishnanethi.agamotto.entities.enums.ProjectRole;
import com.srikrishnanethi.agamotto.exception.ConflictException;
import com.srikrishnanethi.agamotto.exception.ForbiddenException;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.*;
import com.srikrishnanethi.agamotto.security.AgamottoSecurity;
import com.srikrishnanethi.agamotto.service.CollaborationService;
import com.srikrishnanethi.agamotto.service.ProjectAccessService;
import com.srikrishnanethi.agamotto.service.realtime.ProjectEventPublisher;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class CollaborationServiceImpl implements CollaborationService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInviteRepository projectInviteRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ProjectAccessService projectAccessService;
    private final ProjectEventPublisher projectEventPublisher;

    public CollaborationServiceImpl(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectInviteRepository projectInviteRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            ProjectAccessService projectAccessService,
            ProjectEventPublisher projectEventPublisher) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectInviteRepository = projectInviteRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.projectAccessService = projectAccessService;
        this.projectEventPublisher = projectEventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMember> listMembers(String projectId) {
        projectAccessService.requireView(AgamottoSecurity.currentUserId(), projectId);
        List<ProjectMember> members = projectMemberRepository.findByProjectIdOrderByJoinedAtAsc(projectId);
        members.forEach(CollaborationServiceImpl::initializeMember);
        return members;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectInvite> listPendingInvites(String projectId) {
        projectAccessService.requireOwner(AgamottoSecurity.currentUserId(), projectId);
        List<ProjectInvite> invites =
                projectInviteRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(
                        projectId, InviteStatus.PENDING);
        invites.forEach(CollaborationServiceImpl::initializeInvite);
        return invites;
    }

    @Override
    @Transactional
    public ProjectInvite invite(String projectId, String inviteeEmail, ProjectRole role) {
        Objects.requireNonNull(inviteeEmail, "inviteeEmail");
        Objects.requireNonNull(role, "role");
        if (role == ProjectRole.OWNER) {
            throw new IllegalArgumentException("Cannot invite as OWNER");
        }

        String actorId = AgamottoSecurity.currentUserId();
        projectAccessService.requireOwner(actorId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        User inviter = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actorId));

        String normalizedEmail = inviteeEmail.trim().toLowerCase(Locale.ROOT);
        User invitee = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No registered user with email: " + normalizedEmail));

        if (invitee.getId().equals(actorId)) {
            throw new ConflictException("Cannot invite yourself");
        }
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, invitee.getId())) {
            throw new ConflictException("User is already a member of this project");
        }
        if (projectInviteRepository
                .findByProjectIdAndInviteeEmailIgnoreCaseAndStatus(
                        projectId, normalizedEmail, InviteStatus.PENDING)
                .isPresent()) {
            throw new ConflictException("Pending invite already exists for " + normalizedEmail);
        }

        ProjectInvite invite = new ProjectInvite();
        invite.setProject(project);
        invite.setInviter(inviter);
        invite.setInviteeEmail(normalizedEmail);
        invite.setRole(role);
        invite.setStatus(InviteStatus.PENDING);
        invite.setCreatedAt(Instant.now());
        invite = projectInviteRepository.save(invite);

        Notification notification = new Notification();
        notification.setUser(invitee);
        notification.setProject(project);
        notification.setInvite(invite);
        notification.setType(NotificationType.PROJECT_INVITE);
        notification.setMessage("You were invited to project '" + project.getName()
                + "' as " + role.name() + " by " + inviter.getFullName());
        notification.setCreatedAt(Instant.now());
        notification.setSentAt(Instant.now());
        notification.setRead(false);
        notificationRepository.save(notification);

        initializeInvite(invite);
        projectEventPublisher.publishMembersChanged(projectId, actorId);
        return invite;
    }

    @Override
    @Transactional
    public void cancelInvite(String projectId, String inviteId) {
        String actorId = AgamottoSecurity.currentUserId();
        projectAccessService.requireOwner(actorId, projectId);
        ProjectInvite invite = requireInviteOnProject(projectId, inviteId);
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ConflictException("Invite is not pending");
        }
        invite.setStatus(InviteStatus.CANCELLED);
        invite.setResolvedAt(Instant.now());
        projectInviteRepository.save(invite);
        projectEventPublisher.publishMembersChanged(projectId, actorId);
    }

    @Override
    @Transactional
    public ProjectInvite acceptInvite(String inviteId) {
        String actorId = AgamottoSecurity.currentUserId();
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actorId));

        ProjectInvite invite = projectInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found: " + inviteId));
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ConflictException("Invite is not pending");
        }
        if (!actor.getEmail().equalsIgnoreCase(invite.getInviteeEmail())) {
            throw new ForbiddenException("Invite is not addressed to the current user");
        }

        Project project = invite.getProject();
        if (projectMemberRepository.existsByProjectIdAndUserId(project.getId(), actorId)) {
            invite.setStatus(InviteStatus.ACCEPTED);
            invite.setResolvedAt(Instant.now());
            projectInviteRepository.save(invite);
            initializeInvite(invite);
            return invite;
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(actor);
        member.setRole(invite.getRole());
        member.setJoinedAt(Instant.now());
        projectMemberRepository.save(member);

        invite.setStatus(InviteStatus.ACCEPTED);
        invite.setResolvedAt(Instant.now());
        projectInviteRepository.save(invite);

        markInviteNotificationsRead(inviteId);

        initializeInvite(invite);
        projectEventPublisher.publishMembersChanged(project.getId(), actorId);
        return invite;
    }

    @Override
    @Transactional
    public ProjectInvite declineInvite(String inviteId) {
        String actorId = AgamottoSecurity.currentUserId();
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + actorId));

        ProjectInvite invite = projectInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found: " + inviteId));
        if (invite.getStatus() != InviteStatus.PENDING) {
            throw new ConflictException("Invite is not pending");
        }
        if (!actor.getEmail().equalsIgnoreCase(invite.getInviteeEmail())) {
            throw new ForbiddenException("Invite is not addressed to the current user");
        }

        invite.setStatus(InviteStatus.DECLINED);
        invite.setResolvedAt(Instant.now());
        projectInviteRepository.save(invite);
        markInviteNotificationsRead(inviteId);
        initializeInvite(invite);
        return invite;
    }

    @Override
    @Transactional
    public ProjectMember updateMemberRole(String projectId, String userId, ProjectRole role) {
        Objects.requireNonNull(role, "role");
        String actorId = AgamottoSecurity.currentUserId();
        projectAccessService.requireOwner(actorId, projectId);

        if (role == ProjectRole.OWNER) {
            throw new IllegalArgumentException("Use ownership transfer to assign OWNER");
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member not found on project: " + userId));

        if (member.getRole() == ProjectRole.OWNER) {
            throw new ConflictException("Cannot change the project owner's role");
        }

        member.setRole(role);
        ProjectMember saved = projectMemberRepository.save(member);
        initializeMember(saved);
        projectEventPublisher.publishMembersChanged(projectId, actorId);
        return saved;
    }

    @Override
    @Transactional
    public void removeMember(String projectId, String userId) {
        String actorId = AgamottoSecurity.currentUserId();
        projectAccessService.requireOwner(actorId, projectId);

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member not found on project: " + userId));
        if (member.getRole() == ProjectRole.OWNER) {
            throw new ConflictException("Cannot remove the project owner");
        }
        projectMemberRepository.delete(member);
        projectEventPublisher.publishMembersChanged(projectId, actorId);
    }

    @Override
    @Transactional
    public void leaveProject(String projectId) {
        String actorId = AgamottoSecurity.currentUserId();
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, actorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member not found on project: " + actorId));
        if (member.getRole() == ProjectRole.OWNER) {
            throw new ConflictException("Owner cannot leave the project; delete it or transfer ownership");
        }
        projectMemberRepository.delete(member);
        projectEventPublisher.publishMembersChanged(projectId, actorId);
    }

    private ProjectInvite requireInviteOnProject(String projectId, String inviteId) {
        ProjectInvite invite = projectInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found: " + inviteId));
        if (!projectId.equals(invite.getProject().getId())) {
            throw new ResourceNotFoundException("Invite not found on project: " + inviteId);
        }
        return invite;
    }

    private void markInviteNotificationsRead(String inviteId) {
        for (Notification notification : notificationRepository.findByInviteId(inviteId)) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    private static void initializeMember(ProjectMember member) {
        if (member.getUser() != null) {
            Hibernate.initialize(member.getUser());
        }
        if (member.getProject() != null) {
            Hibernate.initialize(member.getProject());
        }
    }

    private static void initializeInvite(ProjectInvite invite) {
        if (invite.getProject() != null) {
            Hibernate.initialize(invite.getProject());
        }
        if (invite.getInviter() != null) {
            Hibernate.initialize(invite.getInviter());
        }
    }
}
