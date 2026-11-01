package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.TaskDependency;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import com.srikrishnanethi.agamotto.entities.enums.ScheduleMode;
import com.srikrishnanethi.agamotto.repositories.TaskDependencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SchedulerEngineTest {

	private SchedulerEngine engine;
	private ScheduleBuilder scheduleBuilder;
	private DependencyGraphService dependencyGraph;

	@BeforeEach
	void setUp() {
		ScoringStrategy scoring = new ScoringStrategy();
		TaskDependencyRepository depRepo = mock(TaskDependencyRepository.class);
		dependencyGraph = new DependencyGraphService(depRepo);
		scheduleBuilder = new ScheduleBuilder(scoring, dependencyGraph);
		engine = new SchedulerEngine(scoring, new BestFitSelector(scoring), scheduleBuilder, dependencyGraph);
	}

	@Test
	void nonFiniteScoreWeightsThrowValidationError() {
		UserProfile profile = weekdayNineToFive();
		profile.setWeightPriority(Double.NaN);
		LocalDate start = LocalDate.of(2026, 8, 10);
		Task task = task("t1", "A", 3, hoursFrom(start, 2, 17), 2.0, 2);

		assertThrows(IllegalArgumentException.class,
				() -> engine.generate(List.of(task), start, start.plusDays(2), profile));
	}

	@Test
	void unsavedTasksWithNullIdsAreAllPlaced() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		List<Task> tasks = List.of(
				task(null, "A", 3, hoursFrom(start, 2, 17), 2.0, 2),
				task(null, "B", 3, hoursFrom(start, 2, 17), 2.0, 2),
				task(null, "C", 3, hoursFrom(start, 2, 17), 2.0, 2));

		ScheduleResult result = engine.generate(tasks, start, start.plusDays(4), profile);

		long scheduled = result.blocks().stream()
				.filter(b -> b.getDecision() == BlockDecision.SCHEDULED)
				.map(ScheduleBlock::getTask)
				.distinct()
				.count();
		assertEquals(3, scheduled);
	}

	@Test
	void fewTasksHappyPathPlacesAllInSerenity() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10); // Monday
		LocalDate end = LocalDate.of(2026, 8, 14); // Friday
		List<Task> tasks = List.of(
				task("t1", "Essay", 3, hoursFrom(start, 5, 17), 2.0, 2),
				task("t2", "Lab write-up", 4, hoursFrom(start, 4, 12), 3.0, 3),
				task("t3", "Reading", 2, hoursFrom(start, 6, 9), 1.5, 1));

		ScheduleResult result = engine.generate(tasks, start, end, profile);

		assertEquals(ScheduleMode.SERENITY, result.mode());
		assertFalse(result.blocks().isEmpty());
		assertEquals(3, distinctTaskIds(result, BlockDecision.SCHEDULED).size());
		assertTrue(result.blocks().stream().noneMatch(b -> b.getDecision() == BlockDecision.EXCLUDED));
		assertNoZeroLengthScheduledBlocks(result);
		assertScheduledBlocksStayInsideWindow(result, start, end, profile);
	}

	@Test
	void fewTasksOverCapacityUsesCrunchWithoutThrowing() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 12); // Wednesday
		LocalDate end = start;
		List<Task> tasks = List.of(
				task("t1", "Huge A", 5, hoursFrom(start, 1, 17), 20.0, 3),
				task("t2", "Huge B", 3, hoursFrom(start, 1, 17), 20.0, 3),
				task("t3", "Huge C", 1, hoursFrom(start, 1, 17), 20.0, 3));

		ScheduleResult result = engine.generate(tasks, start, end, profile);

		assertEquals(ScheduleMode.CRUNCH, result.mode());
		assertFalse(result.blocks().isEmpty());
		assertTrue(result.blocks().stream().anyMatch(b -> b.getDecision() == BlockDecision.EXCLUDED));
		assertTrue(result.blocks().stream().anyMatch(b -> b.getDecision() == BlockDecision.SCHEDULED
				|| b.getDecision() == BlockDecision.DELAYED));
		assertNoZeroLengthScheduledBlocks(result);
	}

	@Test
	void highComplexityTasksSplitSessionsWithoutThrowing() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		LocalDate end = LocalDate.of(2026, 8, 12);
		List<Task> tasks = List.of(
				task("t1", "Hard essay", 5, hoursFrom(start, 3, 17), 5.0, 5),
				task("t2", "Hard lab", 4, hoursFrom(start, 3, 17), 4.0, 4));

		ScheduleResult result = engine.generate(tasks, start, end, profile);

		List<ScheduleBlock> scheduled = result.blocks().stream()
				.filter(b -> b.getDecision() == BlockDecision.SCHEDULED)
				.toList();
		assertTrue(scheduled.size() >= 2);
		for (ScheduleBlock block : scheduled) {
			double hours = minutesBetween(block) / 60.0;
			assertTrue(hours <= ScheduleBuilder.MAX_HIGH_COMPLEXITY_SESSION_HOURS + 1e-6,
					"high-complexity session exceeded cap: " + hours);
		}
		assertNoZeroLengthScheduledBlocks(result);
	}

	@Test
	void tenHourMathBlockAcrossFiveDaysIsPacedSensiblyAndNotRequeuedToday() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10); // Monday
		LocalDate end = LocalDate.of(2026, 8, 14); // Friday (5 working days)
		Task maths = task("t-maths", "10 Hour Maths Block", 4, hoursFrom(start, 4, 17), 10.0, 5); // 10h, complexity 5 (2h cap)

		ScheduleResult result = engine.generate(List.of(maths), start, end, profile);

		List<ScheduleBlock> scheduled = result.blocks().stream()
				.filter(b -> b.getDecision() == BlockDecision.SCHEDULED)
				.toList();

		// Paced at 2h/day across 5 days: 5 sessions scheduled on 5 distinct days
		assertEquals(5, scheduled.size());
		for (int i = 0; i < 5; i++) {
			ScheduleBlock block = scheduled.get(i);
			assertEquals(start.plusDays(i), block.getStartTime().toLocalDate());
			double sessionHours = minutesBetween(block) / 60.0;
			assertEquals(2.0, sessionHours, 1e-6);
		}
	}

	@Test
	void missingDeadlineThrowsValidationErrorNotNpe() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		Task missing = task("t1", "No deadline", 3, hoursFrom(start, 2, 17), 2.0, 2);
		missing.setDeadline(null);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> engine.generate(List.of(missing), start, start.plusDays(4), profile));
		assertFalse(ex.getMessage() == null || ex.getMessage().isBlank());
		assertFalse(ex.getClass().equals(NullPointerException.class));
	}

	@Test
	void nullDeadlineInScoringThrowsIllegalArgumentNotNpe() {
		ScoringStrategy scoring = new ScoringStrategy();
		Task task = task("t1", "No deadline", 3, LocalDateTime.of(2026, 8, 12, 17, 0), 2.0, 2);
		task.setDeadline(null);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> scoring.deadlineUrgency(task, LocalDate.of(2026, 8, 10)));
		assertNotNull(ex.getMessage());
	}

	@Test
	void zeroDurationDoesNotThrowAndDoesNotCreateZeroLengthBlocks() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		Task zero = task("t1", "Empty", 3, hoursFrom(start, 2, 17), 0.0, 2);
		Task real = task("t2", "Real", 3, hoursFrom(start, 2, 17), 2.0, 2);

		ScheduleResult result = engine.generate(List.of(zero, real), start, start.plusDays(4), profile);

		assertNotNull(result);
		assertNoZeroLengthScheduledBlocks(result);
		assertEquals(1, distinctTaskIds(result, BlockDecision.SCHEDULED).size());
		assertEquals(List.of("t1"), distinctTaskIds(result, BlockDecision.DELAYED));
		assertTrue(result.blocks().stream()
				.filter(b -> b.getDecision() == BlockDecision.DELAYED)
				.anyMatch(b -> b.getReason() != null && b.getReason().contains("0h")));
	}

	@Test
	void negativeDurationThrowsValidationError() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		Task bad = task("t1", "Negative", 3, hoursFrom(start, 2, 17), -2.0, 2);

		assertThrows(IllegalArgumentException.class,
				() -> engine.generate(List.of(bad), start, start.plusDays(2), profile));
	}

	@Test
	void nanDurationThrowsValidationError() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		Task bad = task("t1", "NaN", 3, hoursFrom(start, 2, 17), Double.NaN, 2);

		assertThrows(IllegalArgumentException.class,
				() -> engine.generate(List.of(bad), start, start.plusDays(2), profile));
	}

	@Test
	void emptyWeekendOnlyWindowDoesNotThrow() {
		UserProfile profile = weekdayNineToFive();
		profile.setWorksOnWeekends(false);
		LocalDate saturday = LocalDate.of(2026, 8, 15);
		LocalDate sunday = LocalDate.of(2026, 8, 16);
		List<Task> tasks = List.of(
				task("t1", "A", 3, hoursFrom(saturday, 2, 17), 2.0, 2),
				task("t2", "B", 2, hoursFrom(saturday, 2, 17), 2.0, 2));

		ScheduleResult result = engine.generate(tasks, saturday, sunday, profile);

		assertNotNull(result);
		assertEquals(0.0, engine.availableHours(saturday, sunday, profile));
		assertNotNull(result.explanationSummary());
		assertTrue(result.explanationSummary().toLowerCase().contains("working day"),
				result.explanationSummary());
		assertEquals(2, result.blocks().size());
		assertTrue(result.blocks().stream().allMatch(b ->
				b.getDecision() == BlockDecision.EXCLUDED || b.getDecision() == BlockDecision.DELAYED));
	}

	@Test
	void invertedWorkWindowThrowsValidationErrorNotNpe() {
		UserProfile profile = weekdayNineToFive();
		profile.setPreferredStart(LocalTime.of(17, 0));
		profile.setPreferredEnd(LocalTime.of(9, 0));
		LocalDate start = LocalDate.of(2026, 8, 10);
		Task task = task("t1", "A", 3, hoursFrom(start, 2, 17), 2.0, 2);

		assertThrows(IllegalArgumentException.class,
				() -> engine.generate(List.of(task), start, start.plusDays(2), profile));
	}

	@Test
	void emptyWorkWindowThrowsValidationError() {
		UserProfile profile = weekdayNineToFive();
		profile.setPreferredStart(LocalTime.of(9, 0));
		profile.setPreferredEnd(LocalTime.of(9, 0));
		LocalDate start = LocalDate.of(2026, 8, 10);
		Task task = task("t1", "A", 3, hoursFrom(start, 2, 17), 2.0, 2);

		assertThrows(IllegalArgumentException.class,
				() -> engine.generate(List.of(task), start, start.plusDays(2), profile));
	}

	@Test
	void overdueDeadlineDoesNotThrow() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		Task overdue = task("t1", "Late", 5, start.minusDays(3).atTime(17, 0), 2.0, 2);
		Task upcoming = task("t2", "Soon", 3, hoursFrom(start, 2, 17), 2.0, 2);

		ScheduleResult result = engine.generate(List.of(overdue, upcoming), start, start.plusDays(4), profile);

		assertNotNull(result);
		assertEquals(2, distinctTaskIds(result, BlockDecision.SCHEDULED).size());
	}

	@Test
	void overdueUrgencyIsStrictlyHigherThanDueTomorrow() {
		ScoringStrategy scoring = new ScoringStrategy();
		LocalDate asOf = LocalDate.of(2026, 8, 10);
		Task overdue = task("t1", "Late", 3, asOf.minusDays(3).atTime(17, 0), 2.0, 2);
		Task dueToday = task("t2", "Today", 3, asOf.atTime(17, 0), 2.0, 2);
		Task dueTomorrow = task("t3", "Tomorrow", 3, asOf.plusDays(1).atTime(17, 0), 2.0, 2);

		double overdueUrgency = scoring.deadlineUrgency(overdue, asOf);
		double todayUrgency = scoring.deadlineUrgency(dueToday, asOf);
		double tomorrowUrgency = scoring.deadlineUrgency(dueTomorrow, asOf);

		assertEquals(1.0, tomorrowUrgency, 1e-9);
		assertEquals(ScoringStrategy.MAX_DEADLINE_URGENCY, overdueUrgency, 1e-9);
		assertEquals(ScoringStrategy.MAX_DEADLINE_URGENCY, todayUrgency, 1e-9);
		assertTrue(overdueUrgency > tomorrowUrgency);
		assertTrue(todayUrgency > tomorrowUrgency);
	}

	@Test
	void localDateMaxRangeThrowsValidationErrorNotDateTimeException() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		Task task = task("t1", "A", 3, hoursFrom(start, 2, 17), 2.0, 2);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> engine.generate(List.of(task), start, LocalDate.MAX, profile));
		assertNotNull(ex.getMessage());
		assertFalse(ex.getMessage().isBlank());
	}

	@Test
	void oversizedPlanRangeThrowsValidationError() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		LocalDate end = start.plusDays(SchedulerEngine.MAX_PLAN_DAYS);
		Task task = task("t1", "A", 3, hoursFrom(start, 2, 17), 2.0, 2);

		assertThrows(IllegalArgumentException.class,
				() -> engine.generate(List.of(task), start, end, profile));
	}

	@Test
	void nullTasksThrowsValidationErrorNotNpeFromSelectMode() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);

		assertThrows(NullPointerException.class,
				() -> engine.generate(null, start, start.plusDays(2), profile));
	}

	@Test
	void nullProfileThrowsValidationError() {
		LocalDate start = LocalDate.of(2026, 8, 10);
		Task task = task("t1", "A", 3, hoursFrom(start, 2, 17), 2.0, 2);

		assertThrows(NullPointerException.class,
				() -> engine.generate(List.of(task), start, start.plusDays(2), null));
	}

	@Test
	void invertedDateRangeThrowsValidationError() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 14);
		LocalDate end = LocalDate.of(2026, 8, 10);
		Task task = task("t1", "A", 3, hoursFrom(end, 2, 17), 2.0, 2);

		assertThrows(IllegalArgumentException.class,
				() -> engine.generate(List.of(task), start, end, profile));
	}

	@Test
	void emptyTaskListReturnsEmptySerenityPlan() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);

		ScheduleResult result = engine.generate(List.of(), start, start.plusDays(4), profile);

		assertEquals(ScheduleMode.SERENITY, result.mode());
		assertTrue(result.blocks().isEmpty());
	}

	@Test
	void subMinuteRemainderDoesNotCreateZeroLengthBlocks() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		Task tiny = task("t1", "Tiny", 3, hoursFrom(start, 2, 17), 0.001, 2);

		ScheduleResult result = engine.generate(List.of(tiny), start, start.plusDays(2), profile);

		assertNoZeroLengthScheduledBlocks(result);
	}

	@Test
	void scheduledBlocksDoNotCrossPreferredEnd() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		List<Task> tasks = List.of(
				task("t1", "A", 3, hoursFrom(start, 5, 17), 7.5, 2),
				task("t2", "B", 3, hoursFrom(start, 5, 17), 7.5, 2),
				task("t3", "C", 3, hoursFrom(start, 5, 17), 7.5, 2));

		ScheduleResult result = engine.generate(tasks, start, start.plusDays(4), profile);

		assertScheduledBlocksStayInsideWindow(result, start, start.plusDays(4), profile);
	}

	@Test
	void overlappingDeadlinesAndSamePriorityDoNotThrow() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		LocalDateTime deadline = hoursFrom(start, 3, 17);
		List<Task> tasks = List.of(
				task("t1", "A", 3, deadline, 2.0, 2),
				task("t2", "B", 3, deadline, 2.0, 2),
				task("t3", "C", 3, deadline, 2.0, 2));

		ScheduleResult result = engine.generate(tasks, start, start.plusDays(4), profile);

		assertEquals(3, distinctTaskIds(result, BlockDecision.SCHEDULED).size());
		assertNoZeroLengthScheduledBlocks(result);
	}

	@Test
	void reasonStringsFitScheduleBlockColumn() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		Task task = task("t1", "A", 5, hoursFrom(start, 2, 17), 6.0, 5);

		ScheduleResult result = engine.generate(List.of(task), start, start.plusDays(4), profile);

		for (ScheduleBlock block : result.blocks()) {
			assertNotNull(block.getReason());
			assertTrue(block.getReason().length() <= ScheduleBuilder.MAX_REASON_LENGTH,
					"reason length " + block.getReason().length());
		}
	}

	@Test
	void clampReasonTruncatesToEntityLength() {
		String clamped = ScheduleBuilder.clampReason("x".repeat(1500));
		assertEquals(ScheduleBuilder.MAX_REASON_LENGTH, clamped.length());
		assertTrue(clamped.endsWith("..."));
	}

	private static UserProfile weekdayNineToFive() {
		UserProfile profile = new UserProfile();
		profile.setPreferredStart(LocalTime.of(9, 0));
		profile.setPreferredEnd(LocalTime.of(17, 0));
		profile.setWorksOnWeekends(false);
		profile.setWeightPriority(1.0);
		profile.setWeightUrgency(1.0);
		profile.setWeightDuration(1.0);
		return profile;
	}

	private static Task task(
			String id,
			String title,
			int priority,
			LocalDateTime deadline,
			double hours,
			int complexity) {
		Task task = new Task();
		task.setId(id);
		task.setTitle(title);
		task.setPriority(priority);
		task.setDeadline(deadline);
		task.setEstimatedDurationHours(hours);
		task.setComplexity(complexity);
		return task;
	}

	private static LocalDateTime hoursFrom(LocalDate start, int days, int hour) {
		return start.plusDays(days).atTime(hour, 0);
	}

	private static List<String> distinctTaskIds(ScheduleResult result, BlockDecision decision) {
		return result.blocks().stream()
				.filter(b -> b.getDecision() == decision)
				.map(b -> b.getTask().getId())
				.filter(Objects::nonNull)
				.distinct()
				.toList();
	}

	private static void assertNoZeroLengthScheduledBlocks(ScheduleResult result) {
		for (ScheduleBlock block : result.blocks()) {
			if (block.getDecision() != BlockDecision.SCHEDULED) {
				continue;
			}
			assertNotNull(block.getStartTime());
			assertNotNull(block.getEndTime());
			assertTrue(block.getEndTime().isAfter(block.getStartTime()),
					"zero-length scheduled block " + block.getStartTime());
		}
	}

	private static void assertScheduledBlocksStayInsideWindow(
			ScheduleResult result,
			LocalDate start,
			LocalDate end,
			UserProfile profile) {
		for (ScheduleBlock block : result.blocks()) {
			if (block.getDecision() != BlockDecision.SCHEDULED) {
				continue;
			}
			LocalDate day = block.getStartTime().toLocalDate();
			assertFalse(day.isBefore(start) || day.isAfter(end));
			assertFalse(block.getStartTime().toLocalTime().isBefore(profile.getPreferredStart()));
			assertFalse(block.getEndTime().toLocalTime().isAfter(profile.getPreferredEnd()));
			assertEquals(day, block.getEndTime().toLocalDate());
		}
	}

	private static long minutesBetween(ScheduleBlock block) {
		return java.time.Duration.between(block.getStartTime(), block.getEndTime()).toMinutes();
	}

	@Test
	void rearrangingAfterUnattendedSessionApproachingDeadlineShowsErrorMessageCantDoAnythingAnymore() {
		UserProfile profile = weekdayNineToFive(); // 09:00 - 17:00 (8h/day)
		LocalDate start = LocalDate.of(2026, 8, 10); // Monday
		LocalDate end = LocalDate.of(2026, 8, 14); // Friday

		// Task with deadline on Wednesday Aug 12 at 11:00 AM. Total duration = 4h.
		Task task = task("t1", "Critical Coursework", 4, LocalDateTime.of(2026, 8, 12, 11, 0), 4.0, 3);

		// Prior completed session: Monday Aug 10, 09:00 - 10:00 (1h completed)
		ScheduleBlock completed = new ScheduleBlock();
		completed.setId("b-past");
		completed.setTask(task);
		completed.setStartTime(LocalDateTime.of(2026, 8, 10, 9, 0));
		completed.setEndTime(LocalDateTime.of(2026, 8, 10, 10, 0));
		completed.setDecision(BlockDecision.SCHEDULED);

		// Unattended session: Tuesday Aug 11, 09:00 - 17:00 (user was absent all day)
		ScheduleBlock missed = new ScheduleBlock();
		missed.setId("b-unattended");
		missed.setTask(task);
		missed.setStartTime(LocalDateTime.of(2026, 8, 11, 9, 0));
		missed.setEndTime(LocalDateTime.of(2026, 8, 11, 17, 0));
		missed.setDecision(BlockDecision.SCHEDULED);

		List<ScheduleBlock> existing = List.of(completed, missed);

		// Rearrange starting from the end of the unattended session (Tuesday 17:00)
		List<ScheduleBlock> result = scheduleBuilder.rearrangeForUnattendedSession(existing, missed, end, profile, List.of());

		// Resumption moves to Wednesday Aug 12 (deadline day!).
		// Available before deadline (09:00 - 11:00) is only 2 hours.
		// Remaining required work was 3 hours (4h total - 1h completed on Monday).
		// So 2 hours are placed on Wednesday morning, leaving 1 hour unplaced.
		// The 1 hour CANNOT be deferred past the deadline!
		// It must be marked DELAYED with the error message showing you can't do anything anymore.
		assertTrue(scheduleBuilder.hasDeadlineApproachedError(result));

		ScheduleBlock delayedBlock = result.stream()
				.filter(b -> b.getDecision() == BlockDecision.DELAYED)
				.findFirst()
				.orElseThrow();

		assertNotNull(delayedBlock.getReason());
		assertTrue(delayedBlock.getReason().contains("Delayed: Error: Rearranging schedule after unattended session approached the day of the deadline (2026-08-12)"),
				delayedBlock.getReason());
		assertTrue(delayedBlock.getReason().contains("you can't do anything anymore"),
				delayedBlock.getReason());

		// Verify validateNoDeadlineApproachedError throws the expected IllegalStateException
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> scheduleBuilder.validateNoDeadlineApproachedError(result));
		assertTrue(ex.getMessage().contains("you can't do anything anymore"));
	}

	@Test
	void rearrangingAfterUnattendedSessionWhenMissedSessionEndsPastDeadlineShowsErrorMessage() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		LocalDate end = LocalDate.of(2026, 8, 14);

		// Task deadline was Tuesday Aug 11 at 12:00
		Task task = task("t1", "Past Deadline Task", 3, LocalDateTime.of(2026, 8, 11, 12, 0), 2.0, 2);

		// Unattended session ended at 17:00 on Tuesday Aug 11 (after deadline of 12:00)
		ScheduleBlock missed = new ScheduleBlock();
		missed.setId("b-missed");
		missed.setTask(task);
		missed.setStartTime(LocalDateTime.of(2026, 8, 11, 9, 0));
		missed.setEndTime(LocalDateTime.of(2026, 8, 11, 17, 0));
		missed.setDecision(BlockDecision.SCHEDULED);

		List<ScheduleBlock> result = scheduleBuilder.rearrangeForUnattendedSession(List.of(missed), missed, end, profile, List.of());

		assertTrue(scheduleBuilder.hasDeadlineApproachedError(result));
		ScheduleBlock delayed = result.stream()
				.filter(b -> b.getDecision() == BlockDecision.DELAYED)
				.findFirst()
				.orElseThrow();

		assertTrue(delayed.getReason().contains("The deadline has already arrived or passed; you can't do anything anymore."));
	}

	@Test
	void rearrangingAfterUnattendedSessionWithAmpleTimeReschedulesWithoutDeadlineError() {
		UserProfile profile = weekdayNineToFive();
		LocalDate start = LocalDate.of(2026, 8, 10);
		LocalDate end = LocalDate.of(2026, 8, 21); // 2 weeks window

		// Task deadline is next week (Aug 20)
		Task task = task("t1", "Future Task", 3, LocalDateTime.of(2026, 8, 20, 17, 0), 2.0, 2);

		// Missed session on Monday Aug 10, 09:00 - 11:00
		ScheduleBlock missed = new ScheduleBlock();
		missed.setId("b-missed-early");
		missed.setTask(task);
		missed.setStartTime(LocalDateTime.of(2026, 8, 10, 9, 0));
		missed.setEndTime(LocalDateTime.of(2026, 8, 10, 11, 0));
		missed.setDecision(BlockDecision.SCHEDULED);

		List<ScheduleBlock> result = scheduleBuilder.rearrangeForUnattendedSession(List.of(missed), missed, end, profile, List.of());

		// Should successfully reschedule without deadline-approached error
		assertFalse(scheduleBuilder.hasDeadlineApproachedError(result));
		assertFalse(result.stream().anyMatch(b -> b.getDecision() == BlockDecision.DELAYED));
	}
}
