package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.response.DashboardReportResponse;
import com.srikrishnanethi.agamotto.entities.enums.ReportPeriod;
import com.srikrishnanethi.agamotto.mapper.DashboardMapper;
import com.srikrishnanethi.agamotto.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final DashboardService dashboardService;
    private final DashboardMapper dashboardMapper;

    public DashboardController(DashboardService dashboardService, DashboardMapper dashboardMapper) {
        this.dashboardService = dashboardService;
        this.dashboardMapper = dashboardMapper;
    }

    @GetMapping("/dashboard")
    public DashboardReportResponse getDashboard(@RequestParam ReportPeriod period,
                                                @RequestParam String userId,
                                                @RequestParam(required = false) LocalDate asOf) {
        LocalDate end = asOf != null ? asOf : LocalDate.now();
        return dashboardMapper.toResponse(dashboardService.generateReportForUser(userId, period, end));
    }

    @GetMapping("/projects/{projectId}/reports")
    public DashboardReportResponse getProjectReport(
            @PathVariable String projectId,
            @RequestParam ReportPeriod period,
            @RequestParam(required = false) LocalDate asOf) {
        LocalDate end = asOf != null ? asOf : LocalDate.now();
        return dashboardMapper.toResponse(
                dashboardService.generateReportForProject(projectId, period, end));
    }
}
