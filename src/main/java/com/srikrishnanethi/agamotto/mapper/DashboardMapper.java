package com.srikrishnanethi.agamotto.mapper;

import com.srikrishnanethi.agamotto.dto.response.DashboardReportResponse;
import com.srikrishnanethi.agamotto.entities.DashboardReport;
import org.springframework.stereotype.Component;

@Component
public class DashboardMapper {
    public DashboardReportResponse toResponse(DashboardReport report) {
        return new DashboardReportResponse(
                report.getId(),
                report.getUser().getId(),
                report.getPeriod(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                report.getScheduledCount(),
                report.getDelayedCount(),
                report.getExcludedCount(),
                report.getCompletedCount(),
                report.getGeneratedAt());
    }
}
