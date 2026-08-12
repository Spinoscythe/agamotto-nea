package com.srikrishnanethi.agamotto.service;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.SchedulePlan;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import com.srikrishnanethi.agamotto.service.scheduler.GeneratedSchedule;
import com.srikrishnanethi.agamotto.service.scheduler.RescheduleResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SchedulePlanService {
    GeneratedSchedule generateAndPersist(String projectId, LocalDate startDate, LocalDate endDate);

    SchedulePlan getById(String planId);

    List<SchedulePlan> listByProject(String projectId);

    ScheduleBlock getBlockById(String blockId);

    ScheduleBlock overrideBlock(
            String blockId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BlockDecision decision,
            String reason);

    RescheduleResult rescheduleBlock(
            String blockId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String reason);
}
