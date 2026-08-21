package com.srikrishnanethi.agamotto.service;

import com.srikrishnanethi.agamotto.entities.DashboardReport;
import com.srikrishnanethi.agamotto.entities.enums.ReportPeriod;

import java.time.LocalDate;

public interface DashboardService {
    DashboardReport generateReportForUser(String userId, ReportPeriod period, LocalDate asOf);

    DashboardReport generateReportForProject(String projectId, ReportPeriod period, LocalDate asOf);

    static LocalDate resolvePeriodStart(ReportPeriod period, LocalDate asOf) {
        return switch (period) {
            case DAILY -> asOf;
            case WEEKLY -> asOf.minusDays(6);
            case MONTHLY -> asOf.minusDays(29);
        };
    }
}
