package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import com.srikrishnanethi.agamotto.exception.ScheduleConflictException;
import com.srikrishnanethi.agamotto.repositories.ScheduleBlockRepository;

import java.time.LocalDateTime;
import java.util.List;

final class ScheduleOverlapValidator {

    private final ScheduleBlockRepository scheduleBlockRepository;

    ScheduleOverlapValidator(ScheduleBlockRepository scheduleBlockRepository) {
        this.scheduleBlockRepository = scheduleBlockRepository;
    }

    void assertNoScheduledOverlap(ScheduleBlock moving, LocalDateTime start, LocalDateTime end) {
        String planId = moving.getSchedule().getId();
        List<ScheduleBlock> siblings = scheduleBlockRepository.findByScheduleIdOrderByStartTime(planId);
        for (ScheduleBlock other : siblings) {
            if (other.getId() != null && other.getId().equals(moving.getId())) {
                continue;
            }
            if (other.getDecision() != BlockDecision.SCHEDULED) {
                continue;
            }
            LocalDateTime otherStart = other.getStartTime();
            LocalDateTime otherEnd = other.getEndTime();
            if (otherStart == null || otherEnd == null) {
                continue;
            }
            if (start.isBefore(otherEnd) && otherStart.isBefore(end)) {
                throw new ScheduleConflictException(
                        "Schedule conflict: block overlaps another SCHEDULED block on the same plan"
                                + " (" + otherStart + " – " + otherEnd + ")");
            }
        }
    }
}
