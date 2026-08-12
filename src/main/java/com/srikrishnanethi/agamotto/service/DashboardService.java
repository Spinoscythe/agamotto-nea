package com.srikrishnanethi.agamotto.service;

import com.srikrishnanethi.agamotto.entities.DashboardReport;
import com.srikrishnanethi.agamotto.entities.enums.ReportPeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DashboardService {
    DashboardReport generateReportForUser(String userId, ReportPeriod period, LocalDate asOf);

    DashboardReport generateReportForProject(String projectId, ReportPeriod period, LocalDate asOf);

    Optional<DashboardReport> latestReport(String userId, ReportPeriod period);

    List<DashboardReport> listReports(String userId, ReportPeriod period);

    static LocalDate resolvePeriodStart(ReportPeriod period, LocalDate asOf) {
        return switch (period) {
            case DAILY -> asOf;
            case WEEKLY -> asOf.minusDays(6);
            case MONTHLY -> asOf.minusDays(29);
        };
    }
}
