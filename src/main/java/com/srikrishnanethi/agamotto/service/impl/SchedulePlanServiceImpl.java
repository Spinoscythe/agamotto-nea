package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.SchedulePlan;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.ProjectRepository;
import com.srikrishnanethi.agamotto.repositories.ScheduleBlockRepository;
import com.srikrishnanethi.agamotto.repositories.SchedulePlanRepository;
import com.srikrishnanethi.agamotto.repositories.TaskDependencyRepository;
import com.srikrishnanethi.agamotto.repositories.TaskRepository;
import com.srikrishnanethi.agamotto.repositories.UserProfileRepository;
import com.srikrishnanethi.agamotto.service.NotificationService;
import com.srikrishnanethi.agamotto.service.SchedulePlanService;
import com.srikrishnanethi.agamotto.service.scheduler.GeneratedSchedule;
import com.srikrishnanethi.agamotto.service.scheduler.RescheduleResult;
import com.srikrishnanethi.agamotto.service.scheduler.ScheduleBuilder;
import com.srikrishnanethi.agamotto.service.scheduler.SchedulerEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SchedulePlanServiceImpl implements SchedulePlanService {

    private final SchedulePlanRepository schedulePlanRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final SchedulePlanGenerator schedulePlanGenerator;
    private final ScheduleBlockEditor scheduleBlockEditor;

    public SchedulePlanServiceImpl(ProjectRepository projectRepository,
                                   TaskRepository taskRepository,
                                   TaskDependencyRepository taskDependencyRepository,
                                   UserProfileRepository userProfileRepository,
                                   SchedulePlanRepository schedulePlanRepository,
                                   ScheduleBlockRepository scheduleBlockRepository,
                                   SchedulerEngine schedulerEngine,
                                   NotificationService notificationService) {
        this(projectRepository,
                taskRepository,
                taskDependencyRepository,
                userProfileRepository,
                schedulePlanRepository,
                scheduleBlockRepository,
                schedulerEngine,
                notificationService,
                null);
    }

    @Autowired
    public SchedulePlanServiceImpl(ProjectRepository projectRepository,
                                   TaskRepository taskRepository,
                                   TaskDependencyRepository taskDependencyRepository,
                                   UserProfileRepository userProfileRepository,
                                   SchedulePlanRepository schedulePlanRepository,
                                   ScheduleBlockRepository scheduleBlockRepository,
                                   SchedulerEngine schedulerEngine,
                                   NotificationService notificationService,
                                   @Autowired(required = false) ScheduleBuilder scheduleBuilder) {
        this.schedulePlanRepository = schedulePlanRepository;
        this.scheduleBlockRepository = scheduleBlockRepository;

        ScheduleBuilder resolvedScheduleBuilder = scheduleBuilder != null
                ? scheduleBuilder
                : (schedulerEngine != null ? schedulerEngine.getScheduleBuilder() : null);
        SchedulePlanNotifier notifier = new SchedulePlanNotifier(notificationService);
        ScheduleOverlapValidator overlapValidator = new ScheduleOverlapValidator(scheduleBlockRepository);

        this.schedulePlanGenerator = new SchedulePlanGenerator(
                projectRepository,
                taskRepository,
                taskDependencyRepository,
                userProfileRepository,
                schedulePlanRepository,
                schedulerEngine,
                notifier);
        this.scheduleBlockEditor = new ScheduleBlockEditor(
                userProfileRepository,
                scheduleBlockRepository,
                schedulePlanRepository,
                taskRepository,
                taskDependencyRepository,
                resolvedScheduleBuilder,
                overlapValidator,
                notifier);
    }

    @Override
    @Transactional
    public GeneratedSchedule generateAndPersist(String projectId, LocalDate startDate, LocalDate endDate) {
        return schedulePlanGenerator.generateAndPersist(projectId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public SchedulePlan getById(String planId) {
        SchedulePlan plan = schedulePlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("SchedulePlan not found: " + planId));
        SchedulePlanGraphInitializer.initializePlanGraph(plan);
        return plan;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchedulePlan> listByProject(String projectId) {
        List<SchedulePlan> plans = schedulePlanRepository.findByProjectIdOrderByGeneratedAtDesc(projectId);
        plans.forEach(SchedulePlanGraphInitializer::initializePlanGraph);
        return plans;
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleBlock getBlockById(String blockId) {
        ScheduleBlock block = scheduleBlockRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduleBlock not found: " + blockId));
        SchedulePlanGraphInitializer.initializeBlockGraph(block);
        return block;
    }

    @Override
    @Transactional
    public ScheduleBlock overrideBlock(String blockId,
                                       LocalDateTime startTime,
                                       LocalDateTime endTime,
                                       BlockDecision decision,
                                       String reason,
                                       Boolean completed) {
        return scheduleBlockEditor.overrideBlock(blockId, startTime, endTime, decision, reason, completed);
    }

    @Override
    @Transactional
    public RescheduleResult rescheduleBlock(String blockId, LocalDateTime startTime, LocalDateTime endTime, String reason) {
        return scheduleBlockEditor.rescheduleBlock(blockId, startTime, endTime, reason);
    }

    @Override
    @Transactional
    public GeneratedSchedule markUnattendedAndRearrange(String blockId) {
        return scheduleBlockEditor.markUnattendedAndRearrange(blockId);
    }
}
