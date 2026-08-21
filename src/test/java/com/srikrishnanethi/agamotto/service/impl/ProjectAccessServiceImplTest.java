package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.ProjectMember;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.enums.ProjectRole;
import com.srikrishnanethi.agamotto.exception.ForbiddenException;
import com.srikrishnanethi.agamotto.repositories.ProjectMemberRepository;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceImplTest {

	@Mock
	private ProjectRepository projectRepository;
	@Mock
	private ProjectMemberRepository projectMemberRepository;

	private ProjectAccessServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new ProjectAccessServiceImpl(projectRepository, projectMemberRepository);
	}

	@Test
	void ownerCanEditEvenWithoutMembershipRow() {
		User owner = new User();
		owner.setId("u1");
		Project project = new Project();
		project.setId("p1");
		project.setOwner(owner);
		when(projectRepository.findById("p1")).thenReturn(Optional.of(project));
		when(projectMemberRepository.findByProjectIdAndUserId("p1", "u1")).thenReturn(Optional.empty());
		when(projectRepository.existsById("p1")).thenReturn(true);

		assertEquals(ProjectRole.OWNER, service.requireEdit("u1", "p1"));
	}

	@Test
	void editorCanViewAndEdit() {
		ProjectMember member = new ProjectMember();
		member.setRole(ProjectRole.EDITOR);
		when(projectRepository.findById("p1")).thenReturn(Optional.of(new Project()));
		when(projectMemberRepository.findByProjectIdAndUserId("p1", "u2")).thenReturn(Optional.of(member));
		when(projectRepository.existsById("p1")).thenReturn(true);

		assertEquals(ProjectRole.EDITOR, service.requireView("u2", "p1"));
		assertEquals(ProjectRole.EDITOR, service.requireEdit("u2", "p1"));
		assertThrows(ForbiddenException.class, () -> service.requireOwner("u2", "p1"));
	}

	@Test
	void outsiderCannotView() {
		User owner = new User();
		owner.setId("u1");
		Project project = new Project();
		project.setId("p1");
		project.setOwner(owner);
		when(projectRepository.findById("p1")).thenReturn(Optional.of(project));
		when(projectMemberRepository.findByProjectIdAndUserId("p1", "u9")).thenReturn(Optional.empty());
		when(projectRepository.existsById("p1")).thenReturn(true);

		assertThrows(ForbiddenException.class, () -> service.requireView("u9", "p1"));
	}
}
