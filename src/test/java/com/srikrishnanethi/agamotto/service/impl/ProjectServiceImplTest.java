package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.exception.ConflictException;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import com.srikrishnanethi.agamotto.repositories.SchedulePlanRepository;
import com.srikrishnanethi.agamotto.repositories.TaskRepository;
import com.srikrishnanethi.agamotto.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

	@Mock
	private ProjectRepository projectRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private TaskRepository taskRepository;
	@Mock
	private SchedulePlanRepository schedulePlanRepository;

	private ProjectServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new ProjectServiceImpl(
				projectRepository, userRepository, taskRepository, schedulePlanRepository);
	}

	@Test
	void deleteWithLeftoverSchedulesThrowsConflict() {
		Project project = new Project();
		project.setId("p1");
		when(projectRepository.findById("p1")).thenReturn(Optional.of(project));
		when(taskRepository.findByProjectId("p1")).thenReturn(List.of());
		when(schedulePlanRepository.existsByProjectId("p1")).thenReturn(true);

		ConflictException ex = assertThrows(ConflictException.class, () -> service.delete("p1"));
		assertTrue(ex.getMessage().contains("schedules"));
		verify(projectRepository, never()).delete(project);
	}

	@Test
	void deleteWithExistingTasksThrowsConflict() {
		Project project = new Project();
		project.setId("p1");
		Task task = new Task();
		task.setId("t1");
		when(projectRepository.findById("p1")).thenReturn(Optional.of(project));
		when(taskRepository.findByProjectId("p1")).thenReturn(List.of(task));

		ConflictException ex = assertThrows(ConflictException.class, () -> service.delete("p1"));
		assertTrue(ex.getMessage().contains("tasks"));
		verify(projectRepository, never()).delete(project);
		verify(schedulePlanRepository, never()).existsByProjectId("p1");
	}

	@Test
	void deleteWithoutTasksOrSchedulesSucceeds() {
		Project project = new Project();
		project.setId("p1");
		when(projectRepository.findById("p1")).thenReturn(Optional.of(project));
		when(taskRepository.findByProjectId("p1")).thenReturn(List.of());
		when(schedulePlanRepository.existsByProjectId("p1")).thenReturn(false);

		service.delete("p1");
		verify(projectRepository).delete(project);
	}
}
