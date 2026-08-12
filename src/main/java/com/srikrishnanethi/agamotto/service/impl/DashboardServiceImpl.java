package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.entities.DashboardReport;
import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import com.srikrishnanethi.agamotto.entities.enums.ReportPeriod;
import com.srikrishnanethi.agamotto.entities.enums.TaskStatus;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.repositories.*;
import com.srikrishnanethi.agamotto.service.DashboardService;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final DashboardReportRepository dashboardReportRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public DashboardServiceImpl(DashboardReportRepository dashboardReportRepository, ScheduleBlockRepository scheduleBlockRepository, TaskRepository taskRepository, UserRepository userRepository, ProjectRepository projectRepository) {
        this.dashboardReportRepository = dashboardReportRepository;
        this.scheduleBlockRepository = scheduleBlockRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional
    public DashboardReport generateReportForUser(String userId, ReportPeriod period, LocalDate asOf) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(asOf, "asOf");

        User user = this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        LocalDate start = DashboardService.resolvePeriodStart(period, asOf);
        LocalDate end = asOf;
        Instant from = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<ScheduleBlock> blocks = this.scheduleBlockRepository.findByOwnerIdAndPlanGeneratedAtBetween(userId, from, to);
        int scheduled = 0;
        int delayed = 0;
        int excluded = 0;
        for (ScheduleBlock block : blocks) {
            BlockDecision decision = block.getDecision();
            if (decision == BlockDecision.SCHEDULED) {
                scheduled++;
            }
            else if (decision == BlockDecision.DELAYED) {
                delayed++;
            }
            else if (decision == BlockDecision.EXCLUDED) {
                excluded++;
            }
        }

        int completed = taskRepository
                .findByProjectOwnerIdAndStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
                        userId, TaskStatus.COMPLETED, from, to)
                .size();

        DashboardReport report = new DashboardReport();
        report.setUser(user);
        report.setPeriod(period);
        report.setPeriodStart(start);
        report.setPeriodEnd(end);
        report.setScheduledCount(scheduled);
        report.setDelayedCount(delayed);
        report.setExcludedCount(excluded);
        report.setCompletedCount(completed);
        report.setGeneratedAt(Instant.now());
        DashboardReport saved = dashboardReportRepository.save(report);
        Hibernate.initialize(saved.getUser());
        return saved;
    }

    @Override
    @Transactional
    public DashboardReport generateReportForProject(String projectId, ReportPeriod period, LocalDate asOf) {
        Project project = this.projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
        return this.generateReportForUser(project.getOwner().getId(), period, asOf);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DashboardReport> latestReport(String userId, ReportPeriod period) {
        return this.dashboardReportRepository.findFirstByUserIdAndPeriodOrderByGeneratedAtDesc(userId, period);
    }

    @Override
    public List<DashboardReport> listReports(String userId, ReportPeriod period) {
        return this.dashboardReportRepository.findByUserIdAndPeriodOrderByGeneratedAtDesc(userId, period);
    }
}
