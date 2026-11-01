package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.repositories.NotificationRepository;
import com.srikrishnanethi.agamotto.repositories.ProjectInviteRepository;
import com.srikrishnanethi.agamotto.repositories.ProjectMemberRepository;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import com.srikrishnanethi.agamotto.repositories.ScheduleBlockRepository;
import com.srikrishnanethi.agamotto.repositories.SchedulePlanRepository;
import com.srikrishnanethi.agamotto.repositories.TaskHistoryRepository;
import com.srikrishnanethi.agamotto.repositories.TaskRepository;
import com.srikrishnanethi.agamotto.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
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
	private TaskHistoryRepository taskHistoryRepository;
	@Mock
	private SchedulePlanRepository schedulePlanRepository;
	@Mock
	private ScheduleBlockRepository scheduleBlockRepository;
	@Mock
	private ProjectMemberRepository projectMemberRepository;
	@Mock
	private ProjectInviteRepository projectInviteRepository;
	@Mock
	private NotificationRepository notificationRepository;

	private ProjectServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new ProjectServiceImpl(
				projectRepository,
				userRepository,
				taskRepository,
				taskHistoryRepository,
				schedulePlanRepository,
				scheduleBlockRepository,
				projectMemberRepository,
				projectInviteRepository,
				notificationRepository);
	}

	@Test
	void createRejectsEndDateBeforeStartDate() {
		User owner = new User();
		owner.setId("u1");
		when(userRepository.findById("u1")).thenReturn(Optional.of(owner));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(
				"u1",
				"Project",
				null,
				LocalDate.of(2026, 11, 30),
				LocalDate.of(2026, 11, 1)));

		assertTrue(ex.getMessage().contains("endDate must be on or after startDate"));
	}

	@Test
	void deleteWithTasksAndSchedulesCascadesChildrenInFkSafeOrder() {
		Project project = new Project();
		project.setId("p1");
		when(projectRepository.findById("p1")).thenReturn(Optional.of(project));

		service.delete("p1");

		InOrder order = inOrder(notificationRepository, projectInviteRepository,
				scheduleBlockRepository, taskHistoryRepository, taskRepository,
				schedulePlanRepository, projectMemberRepository, projectRepository);
		order.verify(notificationRepository).deleteByProjectId("p1");
		order.verify(notificationRepository).deleteByTaskProjectId("p1");
		order.verify(projectInviteRepository).deleteByProjectId("p1");
		order.verify(scheduleBlockRepository).deleteByProjectId("p1");
		order.verify(scheduleBlockRepository).deleteByTaskProjectId("p1");
		order.verify(taskHistoryRepository).deleteByProjectId("p1");
		order.verify(taskRepository).deleteByProjectId("p1");
		order.verify(schedulePlanRepository).deleteByProjectId("p1");
		order.verify(projectMemberRepository).deleteByProjectId("p1");
		order.verify(projectRepository).delete(project);
	}

	@Test
	void deleteWithoutTasksOrSchedulesSucceeds() {
		Project project = new Project();
		project.setId("p1");
		when(projectRepository.findById("p1")).thenReturn(Optional.of(project));

		service.delete("p1");

		verify(notificationRepository).deleteByProjectId("p1");
		verify(projectInviteRepository).deleteByProjectId("p1");
		verify(projectMemberRepository).deleteByProjectId("p1");
		verify(projectRepository).delete(project);
	}
}
