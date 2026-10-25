package com.srikrishnanethi.agamotto.service.scheduler.group;

import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.UserProfile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Tracks the day-by-day timetable and capacity budget for an individual project member.
 */
public class MemberSchedule {
    private final User user;
    private final UserProfile profile;
    // total amount of hours a user can contribute within the timeframe of startDate to endDate
    private final double totalWindowCapacityHours;
    private double scheduledHours = 0.0;

    private LocalDate currentDay;
    private LocalDateTime nextFreeTime;
    private double freeHoursToday;

    public MemberSchedule(User user, UserProfile profile, LocalDate start, double totalWindowCapacityHours) {
        this.user = user;
        this.profile = profile;
        this.totalWindowCapacityHours = totalWindowCapacityHours;
        this.currentDay = start;
        startNewDay(start);
    }

    public void startNewDay(LocalDate day) {
        this.currentDay = day;
        LocalTime start = profile.getPreferredStart();
        LocalTime end = profile.getPreferredEnd();
        this.nextFreeTime = LocalDateTime.of(day, start);
        this.freeHoursToday = ChronoUnit.MINUTES.between(start, end) / 60.0;
    }

    public void bookTime(LocalDateTime nextFreeTime, double hoursBooked) {
        this.nextFreeTime = nextFreeTime;
        this.freeHoursToday = Math.max(0.0, this.freeHoursToday - hoursBooked);
        this.scheduledHours += hoursBooked;
    }

    public double getBusyPercentage() {
        if (totalWindowCapacityHours <= 1e-9) return 1.0;
        return scheduledHours / totalWindowCapacityHours;
    }

    public User getUser() {
        return user;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public LocalDate getCurrentDay() {
        return currentDay;
    }

    public LocalDateTime getNextFreeTime() {
        return nextFreeTime;
    }

    public double getFreeHoursToday() {
        return freeHoursToday;
    }

    public double getScheduledHours() {
        return scheduledHours;
    }

    public double getTotalWindowCapacityHours() {
        return totalWindowCapacityHours;
    }
}
