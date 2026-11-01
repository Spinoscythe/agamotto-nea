package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.exception.ForbiddenException;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceImplTest {

	@Mock
	private ProjectRepository projectRepository;

	private ProjectAccessServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new ProjectAccessServiceImpl(projectRepository);
	}

	@Test
	void ownerCanAccessProject() {
		User owner = new User();
		owner.setId("u1");
		Project project = new Project();
		project.setId("p1");
		project.setOwner(owner);
		when(projectRepository.findById("p1")).thenReturn(Optional.of(project));

		assertDoesNotThrow(() -> service.requireAccess("u1", "p1"));
		assertDoesNotThrow(() -> service.requireOwner("u1", "p1"));
		assertTrue(service.canAccess("u1", "p1"));
	}

	@Test
	void outsiderCannotAccessProject() {
		User owner = new User();
		owner.setId("u1");
		Project project = new Project();
		project.setId("p1");
		project.setOwner(owner);
		when(projectRepository.findById("p1")).thenReturn(Optional.of(project));

		assertThrows(ForbiddenException.class, () -> service.requireAccess("u9", "p1"));
		assertThrows(ForbiddenException.class, () -> service.requireOwner("u9", "p1"));
		assertFalse(service.canAccess("u9", "p1"));
	}
}
