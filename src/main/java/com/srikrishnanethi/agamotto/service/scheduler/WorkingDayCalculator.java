package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.UserProfile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

final class WorkingDayCalculator {

    private WorkingDayCalculator() {
    }

    static double workingHoursPerDay(UserProfile userProfile) {
        Objects.requireNonNull(userProfile, "userProfile");
        if (userProfile.getPreferredStart() == null || userProfile.getPreferredEnd() == null) {
            throw new IllegalArgumentException("preferredStart and preferredEnd are required");
        }
        double hours = ChronoUnit.MINUTES.between(userProfile.getPreferredStart(), userProfile.getPreferredEnd()) / 60.0;
        if (hours <= 0) {
            throw new IllegalArgumentException("preferredEnd must be after preferredStart");
        }
        return hours;
    }

    static long countWorkingDaysUntilDeadline(
            LocalDate currentDay,
            Task task,
            LocalDate planEnd,
            UserProfile profile) {
        LocalDate deadlineDate = task.getDeadline().toLocalDate();
        LocalDate targetEnd = deadlineDate.isBefore(planEnd) ? deadlineDate : planEnd;
        if (currentDay.isAfter(targetEnd)) {
            return 1;
        }

        long workingDays = 0;
        LocalDate d = currentDay;
        while (!d.isAfter(targetEnd)) {
            if (profile.worksOnWeekends() || !isWeekend(d)) {
                workingDays++;
            }
            if (d.equals(LocalDate.MAX)) {
                break;
            }
            d = d.plusDays(1);
        }
        return Math.max(1, workingDays);
    }

    static LocalDate nextDayOrNull(LocalDate day) {
        if (day.equals(LocalDate.MAX)) {
            return null;
        }
        return day.plusDays(1);
    }

    static boolean isWeekend(LocalDate day) {
        return day.getDayOfWeek().getValue() >= 6;
    }
}
