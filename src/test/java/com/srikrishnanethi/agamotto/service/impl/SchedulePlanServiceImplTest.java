package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.SchedulePlan;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import com.srikrishnanethi.agamotto.entities.enums.ScheduleMode;
import com.srikrishnanethi.agamotto.entities.enums.TaskStatus;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import com.srikrishnanethi.agamotto.repositories.ScheduleBlockRepository;
import com.srikrishnanethi.agamotto.repositories.SchedulePlanRepository;
import com.srikrishnanethi.agamotto.repositories.TaskDependencyRepository;
import com.srikrishnanethi.agamotto.repositories.TaskRepository;
import com.srikrishnanethi.agamotto.repositories.UserProfileRepository;
import com.srikrishnanethi.agamotto.service.scheduler.GeneratedSchedule;
import com.srikrishnanethi.agamotto.service.scheduler.RescheduleResult;
import com.srikrishnanethi.agamotto.service.scheduler.ScheduleResult;
import com.srikrishnanethi.agamotto.service.scheduler.SchedulerEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulePlanServiceImplTest {

	@Mock
	private ProjectRepository projectRepository;
	@Mock
	private TaskRepository taskRepository;
	@Mock
	private TaskDependencyRepository taskDependencyRepository;
	@Mock
	private UserProfileRepository userProfileRepository;
	@Mock
	private SchedulePlanRepository schedulePlanRepository;
	@Mock
	private ScheduleBlockRepository scheduleBlockRepository;
	@Mock
	private SchedulerEngine schedulerEngine;
	@Mock
	private com.srikrishnanethi.agamotto.service.NotificationService notificationService;
	@Mock
	private com.srikrishnanethi.agamotto.service.scheduler.ScheduleBuilder scheduleBuilder;

	private SchedulePlanServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new SchedulePlanServiceImpl(
				projectRepository,
				taskRepository,
				taskDependencyRepository,
				userProfileRepository,
				schedulePlanRepository,
				scheduleBlockRepository,
				schedulerEngine,
				notificationService,
				scheduleBuilder);
	}

	@Test
	void generateAndPersistAttachesEngineBlocks() {
		User owner = new User();
		owner.setId("u1");
		Project project = new Project();
		project.setId("p1");
		project.setOwner(owner);
		UserProfile profile = new UserProfile();
		profile.setPreferredStart(LocalTime.of(9, 0));
		profile.setPreferredEnd(LocalTime.of(17, 0));
		Task task = new Task();
		task.setId("t1");
		ScheduleBlock block = new ScheduleBlock();
		block.setTask(task);
		block.setDecision(BlockDecision.SCHEDULED);
		block.setReason("ok");

		when(projectRepository.findById("p1")).thenReturn(Optional.of(project));
		when(userProfileRepository.findByUserId("u1")).thenReturn(Optional.of(profile));
		when(taskRepository.findByProjectIdAndStatusIn(eq("p1"), any())).thenReturn(List.of(task));
		when(schedulerEngine.generate(any(), any(), any(), eq(profile)))
				.thenReturn(new ScheduleResult(ScheduleMode.SERENITY, "summary", List.of(block)));
		when(schedulePlanRepository.findByProjectIdOrderByGeneratedAtDesc("p1")).thenReturn(List.of());
		when(schedulePlanRepository.save(any(SchedulePlan.class))).thenAnswer(inv -> inv.getArgument(0));

		GeneratedSchedule generated = service.generateAndPersist(
				"p1", LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 21));

		assertEquals(ScheduleMode.SERENITY, generated.plan().getMode());
		assertEquals(1, generated.plan().getBlocks().size());
		assertEquals(generated.plan(), block.getSchedule());
		assertEquals("summary", generated.explanationSummary());
		assertEquals("summary", generated.plan().getExplanationSummary());
		verify(notificationService).notifyUser(eq(owner), isNull(), contains("SERENITY"));
	}

	@Test
	void emptyRescheduleDoesNotRegeneratePlan() {
		ScheduleBlock block = scheduledBlock();
		when(scheduleBlockRepository.findById("b1")).thenReturn(Optional.of(block));

		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.rescheduleBlock("b1", null, null, null));
		assertTrue(ex.getMessage().contains("startTime"));
		verify(schedulerEngine, never()).generate(any(), any(), any(), any());
		verify(schedulePlanRepository, never()).save(any());
	}

	@Test
	void rescheduleWithTimesMovesBlock() {
		ScheduleBlock block = scheduledBlock();
		when(scheduleBlockRepository.findById("b1")).thenReturn(Optional.of(block));
		when(scheduleBlockRepository.findByScheduleIdOrderByStartTime("plan1")).thenReturn(List.of(block));
		when(scheduleBlockRepository.save(any(ScheduleBlock.class))).thenAnswer(inv -> inv.getArgument(0));

		LocalDateTime start = LocalDateTime.of(2026, 8, 12, 10, 0);
		LocalDateTime end = LocalDateTime.of(2026, 8, 12, 11, 0);
		RescheduleResult result = service.rescheduleBlock("b1", start, end, "moved");

		assertNotNull(result.movedBlock());
		assertNull(result.regenerated());
		assertEquals(start, result.movedBlock().getStartTime());
		assertEquals(end, result.movedBlock().getEndTime());
		assertEquals("moved", result.movedBlock().getReason());
		verify(schedulerEngine, never()).generate(any(), any(), any(), any());
	}

	@Test
	void overrideClampsReasonToColumnLength() {
		ScheduleBlock block = scheduledBlock();
		when(scheduleBlockRepository.findById("b1")).thenReturn(Optional.of(block));
		when(scheduleBlockRepository.findByScheduleIdOrderByStartTime("plan1")).thenReturn(List.of(block));
		when(scheduleBlockRepository.save(any(ScheduleBlock.class))).thenAnswer(inv -> inv.getArgument(0));

		String tooLong = "x".repeat(1200);
		ScheduleBlock saved = service.overrideBlock("b1", null, null, null, tooLong, null);

		assertEquals(1000, saved.getReason().length());
		assertTrue(saved.getReason().endsWith("..."));
	}

	@Test
	void overrideSetsCompletedFlag() {
		ScheduleBlock block = scheduledBlock();
		when(scheduleBlockRepository.findById("b1")).thenReturn(Optional.of(block));
		when(scheduleBlockRepository.findByScheduleIdOrderByStartTime("plan1")).thenReturn(List.of(block));
		when(scheduleBlockRepository.save(any(ScheduleBlock.class))).thenAnswer(inv -> inv.getArgument(0));

		ScheduleBlock saved = service.overrideBlock("b1", null, null, null, null, true);

		assertTrue(saved.isCompleted());
		assertEquals(TaskStatus.COMPLETED, saved.getTask().getStatus());
		verify(taskRepository).save(saved.getTask());
	}

	@Test
	void overrideUnsetsCompletedFlagAndReopensTask() {
		ScheduleBlock block = scheduledBlock();
		block.setCompleted(true);
		block.getTask().setStatus(TaskStatus.COMPLETED);
		when(scheduleBlockRepository.findById("b1")).thenReturn(Optional.of(block));
		when(scheduleBlockRepository.findByScheduleIdOrderByStartTime("plan1")).thenReturn(List.of(block));
		when(scheduleBlockRepository.save(any(ScheduleBlock.class))).thenAnswer(inv -> inv.getArgument(0));

		ScheduleBlock saved = service.overrideBlock("b1", null, null, null, null, false);

		assertFalse(saved.isCompleted());
		assertEquals(TaskStatus.IN_PROGRESS, saved.getTask().getStatus());
		verify(taskRepository).save(saved.getTask());
	}

	private static ScheduleBlock scheduledBlock() {
		Project project = new Project();
		project.setId("p1");
		SchedulePlan plan = new SchedulePlan();
		plan.setId("plan1");
		plan.setProject(project);
		Task task = new Task();
		task.setId("t1");
		ScheduleBlock block = new ScheduleBlock();
		block.setId("b1");
		block.setSchedule(plan);
		block.setTask(task);
		block.setStartTime(LocalDateTime.of(2026, 8, 12, 9, 0));
		block.setEndTime(LocalDateTime.of(2026, 8, 12, 10, 0));
		block.setDecision(BlockDecision.SCHEDULED);
		block.setReason("original");
		return block;
	}

	@Test
	void markUnattendedAndRearrangeSetsDeadlineErrorInSummaryWhenDeadlineApproached() {
		User owner = new User();
		owner.setId("u1");
		Project project = new Project();
		project.setId("p1");
		project.setOwner(owner);

		SchedulePlan plan = new SchedulePlan();
		plan.setId("plan1");
		plan.setProject(project);
		plan.setEndDate(LocalDate.of(2026, 8, 14));

		ScheduleBlock missedBlock = new ScheduleBlock();
		missedBlock.setId("b1");
		missedBlock.setSchedule(plan);

		Task task = new Task();
		task.setTitle("Coursework");

		ScheduleBlock delayedErrorBlock = new ScheduleBlock();
		delayedErrorBlock.setTask(task);
		delayedErrorBlock.setDecision(BlockDecision.DELAYED);
		String errorMsg = "Delayed: Error: Rearranging schedule after unattended session approached the day of the deadline (2026-08-14). Remaining 2.00h cannot be accommodated before deadline — you can't do anything anymore.";
		delayedErrorBlock.setReason(errorMsg);

		UserProfile profile = new UserProfile();
		when(scheduleBlockRepository.findById("b1")).thenReturn(Optional.of(missedBlock));
		when(userProfileRepository.findByUserId("u1")).thenReturn(Optional.of(profile));
		when(scheduleBlockRepository.findByScheduleIdOrderByStartTime("plan1")).thenReturn(List.of(missedBlock));
		when(scheduleBuilder.rearrangeForUnattendedSession(any(), eq(missedBlock), any(), eq(profile), any()))
				.thenReturn(List.of(delayedErrorBlock));
		when(scheduleBuilder.hasDeadlineApproachedError(any())).thenReturn(true);
		when(schedulePlanRepository.save(any(SchedulePlan.class))).thenAnswer(inv -> inv.getArgument(0));

		GeneratedSchedule result = service.markUnattendedAndRearrange("b1");

		assertNotNull(result);
		assertTrue(result.explanationSummary().contains("you can't do anything anymore"));
		assertEquals(errorMsg, result.plan().getExplanationSummary());
	}
}
