package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import com.srikrishnanethi.agamotto.repositories.TaskHistoryRepository;
import com.srikrishnanethi.agamotto.repositories.TaskRepository;
import com.srikrishnanethi.agamotto.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

	@Mock
	private TaskRepository taskRepository;
	@Mock
	private ProjectRepository projectRepository;
	@Mock
	private TaskHistoryRepository taskHistoryRepository;
	@Mock
	private UserRepository userRepository;

	private TaskServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new TaskServiceImpl(
				taskRepository, projectRepository, taskHistoryRepository, userRepository);
	}

	@Test
	void getByIdThrowsWhenMissing() {
		when(taskRepository.findById("missing")).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> service.getById("missing"));
	}

	@Test
	void listByProjectInitializesProjectForMapper() {
		Project project = new Project();
		project.setId("p1");
		Task task = new Task();
		task.setId("t1");
		task.setProject(project);
		when(taskRepository.findByProjectId("p1")).thenReturn(List.of(task));

		List<Task> listed = service.listByProject("p1");
		assertEquals(1, listed.size());
		assertEquals("p1", listed.getFirst().getProject().getId());
	}

	@Test
	void historyUsesDerivedQueryNotNativeJoin() {
		when(taskHistoryRepository.findByTaskIdOrderByChangedAtDesc("t1")).thenReturn(List.of());
		assertEquals(0, service.historyForTask("t1").size());
	}
}
