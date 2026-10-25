package com.srikrishnanethi.agamotto.service.scheduler.group;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import com.srikrishnanethi.agamotto.service.scheduler.ScoringStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupScheduleBuilderTest {

    private GroupScheduleBuilder builder;

    @BeforeEach
    void setUp() {
        ScoringStrategy scoringStrategy = new ScoringStrategy();
        builder = new GroupScheduleBuilder(scoringStrategy);
    }

    @Test
    void tenHourBlockAcrossFiveDaysIsPacedAndDeferredToTomorrow() {
        LocalDate monday = LocalDate.of(2026, 8, 10);
        LocalDate friday = LocalDate.of(2026, 8, 14);

        User alice = new User();
        alice.setId("u1");
        alice.setFullName("Alice");

        UserProfile profile = new UserProfile();
        profile.setPreferredStart(LocalTime.of(9, 0));
        profile.setPreferredEnd(LocalTime.of(17, 0)); // 8 hours/day
        profile.setIncludeWeekends(false);

        Map<User, UserProfile> team = Map.of(alice, profile);

        Task maths = new Task();
        maths.setId("maths-10h");
        maths.setTitle("10 Hour Maths Block");
        maths.setPriority(4);
        maths.setComplexity(5); // Hard -> max 2h sitting limit
        maths.setEstimatedDurationHours(10.0);
        maths.setDeadline(friday.atTime(17, 0));

        List<ScheduleBlock> blocks = builder.buildSchedule(List.of(maths), monday, friday, team);

        List<ScheduleBlock> scheduled = blocks.stream()
                .filter(b -> b.getDecision() == BlockDecision.SCHEDULED)
                .toList();

        // 10 hours paced over 5 days at 2h/day = 5 distinct daily sessions of 2 hours
        assertEquals(5, scheduled.size());

        for (int i = 0; i < 5; i++) {
            ScheduleBlock block = scheduled.get(i);
            LocalDate expectedDay = monday.plusDays(i);
            assertEquals(expectedDay, block.getStartTime().toLocalDate());
            double sessionHours = Duration.between(block.getStartTime(), block.getEndTime()).toMinutes() / 60.0;
            assertEquals(2.0, sessionHours, 1e-6);
        }
    }

    @Test
    void workloadIsBalancedBetweenMultipleMembers() {
        LocalDate monday = LocalDate.of(2026, 8, 10);
        LocalDate tuesday = LocalDate.of(2026, 8, 11);

        User alice = new User();
        alice.setId("u1");
        alice.setFullName("Alice");

        User bob = new User();
        bob.setId("u2");
        bob.setFullName("Bob");

        UserProfile profile = new UserProfile();
        profile.setPreferredStart(LocalTime.of(9, 0));
        profile.setPreferredEnd(LocalTime.of(17, 0));
        profile.setIncludeWeekends(false);

        Map<User, UserProfile> team = new LinkedHashMap<>();
        team.put(alice, profile);
        team.put(bob, profile);

        Task t1 = new Task();
        t1.setId("task1");
        t1.setTitle("Task 1");
        t1.setPriority(3);
        t1.setComplexity(3); // 1h per sitting
        t1.setEstimatedDurationHours(2.0);
        t1.setDeadline(tuesday.atTime(17, 0));

        Task t2 = new Task();
        t2.setId("task2");
        t2.setTitle("Task 2");
        t2.setPriority(3);
        t2.setComplexity(3); // 1h per sitting
        t2.setEstimatedDurationHours(2.0);
        t2.setDeadline(tuesday.atTime(17, 0));

        List<ScheduleBlock> blocks = builder.buildSchedule(List.of(t1, t2), monday, tuesday, team);

        List<ScheduleBlock> scheduled = blocks.stream()
                .filter(b -> b.getDecision() == BlockDecision.SCHEDULED)
                .toList();

        assertFalse(scheduled.isEmpty());
        // Both Alice and Bob should receive work due to fair share workload balancing
        assertTrue(scheduled.stream().anyMatch(b -> b.getReason().contains("Alice")));
        assertTrue(scheduled.stream().anyMatch(b -> b.getReason().contains("Bob")));
    }
}
