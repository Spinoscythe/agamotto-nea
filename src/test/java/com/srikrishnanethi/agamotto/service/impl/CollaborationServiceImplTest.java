package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.enums.ProjectRole;
import com.srikrishnanethi.agamotto.exception.ForbiddenException;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.NotificationRepository;
import com.srikrishnanethi.agamotto.repositories.ProjectInviteRepository;
import com.srikrishnanethi.agamotto.repositories.ProjectMemberRepository;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import com.srikrishnanethi.agamotto.repositories.UserRepository;
import com.srikrishnanethi.agamotto.security.AgamottoSecurity;
import com.srikrishnanethi.agamotto.service.ProjectAccessService;
import com.srikrishnanethi.agamotto.service.realtime.ProjectEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaborationServiceImplTest {

	@Mock
	private ProjectRepository projectRepository;
	@Mock
	private ProjectMemberRepository projectMemberRepository;
	@Mock
	private ProjectInviteRepository projectInviteRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private NotificationRepository notificationRepository;
	@Mock
	private ProjectAccessService projectAccessService;
	@Mock
	private ProjectEventPublisher projectEventPublisher;

	private CollaborationServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new CollaborationServiceImpl(
				projectRepository,
				projectMemberRepository,
				projectInviteRepository,
				userRepository,
				notificationRepository,
				projectAccessService,
				projectEventPublisher);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(
						"owner-1",
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	@AfterEach
	void clear() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void inviteRejectsUnknownEmail() {
		when(projectAccessService.requireOwner(AgamottoSecurity.currentUserId(), "p1"))
				.thenReturn(ProjectRole.OWNER);
		Project project = new Project();
		project.setId("p1");
		when(projectRepository.findById("p1")).thenReturn(Optional.of(project));
		when(userRepository.findById("owner-1")).thenReturn(Optional.of(owner()));
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> service.invite("p1", "missing@example.com", ProjectRole.EDITOR));
		verify(projectInviteRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void inviteAsOwnerRoleIsRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> service.invite("p1", "b@example.com", ProjectRole.OWNER));
		verify(projectAccessService, never()).requireOwner(org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void outsiderCannotListMembers() {
		when(projectAccessService.requireView("owner-1", "p1"))
				.thenThrow(new ForbiddenException("Not a member of project: p1"));
		assertThrows(ForbiddenException.class, () -> service.listMembers("p1"));
	}

	private static User owner() {
		User user = new User();
		user.setId("owner-1");
		user.setEmail("owner@example.com");
		user.setFullName("Owner");
		return user;
	}
}
