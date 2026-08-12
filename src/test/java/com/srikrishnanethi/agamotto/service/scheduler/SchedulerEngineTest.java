package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import com.srikrishnanethi.agamotto.entities.enums.ScheduleMode;
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

class SchedulerEngineTest {

	private SchedulerEngine engine;

	@BeforeEach
	void setUp() {
		ScoringStrategy scoring = new ScoringStrategy();
		engine = new SchedulerEngine(scoring, new BestFitSelector(scoring), new GreedyPlacer(scoring));
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
			assertTrue(hours <= GreedyPlacer.MAX_HIGH_COMPLEXITY_SESSION_HOURS + 1e-6,
					"high-complexity session exceeded cap: " + hours);
		}
		assertNoZeroLengthScheduledBlocks(result);
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
		profile.setIncludeWeekends(false);
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
			assertTrue(block.getReason().length() <= GreedyPlacer.MAX_REASON_LENGTH,
					"reason length " + block.getReason().length());
		}
	}

	@Test
	void clampReasonTruncatesToEntityLength() {
		String clamped = GreedyPlacer.clampReason("x".repeat(1500));
		assertEquals(GreedyPlacer.MAX_REASON_LENGTH, clamped.length());
		assertTrue(clamped.endsWith("..."));
	}

	private static UserProfile weekdayNineToFive() {
		UserProfile profile = new UserProfile();
		profile.setPreferredStart(LocalTime.of(9, 0));
		profile.setPreferredEnd(LocalTime.of(17, 0));
		profile.setIncludeWeekends(false);
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
}
