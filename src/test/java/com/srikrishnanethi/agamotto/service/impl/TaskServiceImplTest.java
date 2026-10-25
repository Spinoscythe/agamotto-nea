package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.enums.ChangeType;
import com.srikrishnanethi.agamotto.exception.ForbiddenException;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import com.srikrishnanethi.agamotto.repositories.TaskHistoryRepository;
import com.srikrishnanethi.agamotto.repositories.TaskRepository;
import com.srikrishnanethi.agamotto.repositories.UserRepository;
import com.srikrishnanethi.agamotto.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
	@Mock
	private NotificationService notificationService;

	private TaskServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new TaskServiceImpl(
				taskRepository, projectRepository, taskHistoryRepository, userRepository, notificationService);
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

	@Test
	void createWritesCreatedHistory() {
		Project project = new Project();
		project.setId("p1");
		User actor = new User();
		actor.setId("u1");
		when(projectRepository.findById("p1")).thenReturn(Optional.of(project));
		when(userRepository.findById("u1")).thenReturn(Optional.of(actor));
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
			Task task = invocation.getArgument(0);
			task.setId("t1");
			return task;
		});

		service.create(
				"p1",
				"u1",
				"Essay",
				null,
				"work",
				3,
				LocalDateTime.now().plusDays(2),
				2.0,
				2);

		ArgumentCaptor<com.srikrishnanethi.agamotto.entities.TaskHistory> captor =
				ArgumentCaptor.forClass(com.srikrishnanethi.agamotto.entities.TaskHistory.class);
		verify(taskHistoryRepository).save(captor.capture());
		assertEquals(ChangeType.CREATED, captor.getValue().getChangeType());
		assertEquals("u1", captor.getValue().getChangedBy().getId());
	}

	@Test
	void createWithAssigneeAttachesUserAndHistory() {
		Project project = new Project();
		project.setId("p1");
		User actor = new User();
		actor.setId("u1");
		User assignee = new User();
		assignee.setId("u2");
		assignee.setFullName("Bob Collaborator");

		when(projectRepository.findById("p1")).thenReturn(Optional.of(project));
		when(userRepository.findById("u1")).thenReturn(Optional.of(actor));
		when(userRepository.findById("u2")).thenReturn(Optional.of(assignee));
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
			Task task = invocation.getArgument(0);
			task.setId("t2");
			return task;
		});

		Task created = service.create(
				"p1",
				"u1",
				"Group Presentation",
				null,
				"work",
				4,
				LocalDateTime.now().plusDays(3),
				3.0,
				3,
				"u2");

		assertEquals(assignee, created.getAssignee());
		assertEquals("Bob Collaborator", created.getAssignee().getFullName());
		verify(notificationService).notifyUser(eq(assignee), any(Task.class), contains("You have been assigned to task"));
	}

	@Test
	void createWithAssigneeByNonOwnerThrowsForbiddenException() {
		User owner = new User();
		owner.setId("admin-user");

		Project project = new Project();
		project.setId("p1");
		project.setOwner(owner);

		when(projectRepository.findById("p1")).thenReturn(Optional.of(project));

		assertThrows(ForbiddenException.class, () -> service.create(
				"p1",
				"collaborator-user",
				"Task Title",
				null,
				"work",
				3,
				LocalDateTime.now().plusDays(3),
				2.0,
				2,
				"someone-else"));
	}

	@Test
	void updateAssigneeByNonOwnerThrowsForbiddenException() {
		User owner = new User();
		owner.setId("admin-user");

		User collaborator = new User();
		collaborator.setId("collaborator-user");

		Project project = new Project();
		project.setId("p1");
		project.setOwner(owner);

		Task task = new Task();
		task.setId("t1");
		task.setProject(project);
		task.setTitle("Test Task");

		when(taskRepository.findById("t1")).thenReturn(Optional.of(task));
		when(userRepository.findById("collaborator-user")).thenReturn(Optional.of(collaborator));

		assertThrows(ForbiddenException.class, () -> service.update(
				"t1",
				"collaborator-user",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				"new-assignee"));
	}

	@Test
	void updateAssigneeNotifiesNewlyAssignedUser() {
		User owner = new User();
		owner.setId("admin-user");

		Project project = new Project();
		project.setId("p1");
		project.setName("Agamotto Project");
		project.setOwner(owner);

		User newAssignee = new User();
		newAssignee.setId("u2");
		newAssignee.setFullName("Bob Collaborator");

		Task task = new Task();
		task.setId("t1");
		task.setTitle("Prepare Slides");
		task.setProject(project);

		when(taskRepository.findById("t1")).thenReturn(Optional.of(task));
		when(userRepository.findById("admin-user")).thenReturn(Optional.of(owner));
		when(userRepository.findById("u2")).thenReturn(Optional.of(newAssignee));
		when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

		service.update(
				"t1",
				"admin-user",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				"u2");

		verify(notificationService).notifyUser(
				eq(newAssignee),
				eq(task),
				contains("You have been assigned to task 'Prepare Slides' in project 'Agamotto Project'."));
	}
}
