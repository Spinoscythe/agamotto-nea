package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.response.DashboardReportResponse;
import com.srikrishnanethi.agamotto.entities.enums.ReportPeriod;
import com.srikrishnanethi.agamotto.mapper.DashboardMapper;
import com.srikrishnanethi.agamotto.security.AgamottoSecurity;
import com.srikrishnanethi.agamotto.service.DashboardService;
import com.srikrishnanethi.agamotto.service.ProjectAccessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final DashboardService dashboardService;
    private final DashboardMapper dashboardMapper;
    private final ProjectAccessService projectAccessService;

    public DashboardController(
            DashboardService dashboardService,
            DashboardMapper dashboardMapper,
            ProjectAccessService projectAccessService) {
        this.dashboardService = dashboardService;
        this.dashboardMapper = dashboardMapper;
        this.projectAccessService = projectAccessService;
    }

    @GetMapping("/dashboard")
    public DashboardReportResponse getDashboard(@RequestParam ReportPeriod period,
                                                @RequestParam(required = false) String userId,
                                                @RequestParam(required = false) LocalDate asOf) {
        String me = AgamottoSecurity.currentUserId();
        AgamottoSecurity.requireSelf(userId);
        LocalDate end = asOf != null ? asOf : LocalDate.now();
        return dashboardMapper.toResponse(dashboardService.generateReportForUser(me, period, end));
    }

    @GetMapping("/projects/{projectId}/reports")
    public DashboardReportResponse getProjectReport(
            @PathVariable String projectId,
            @RequestParam ReportPeriod period,
            @RequestParam(required = false) LocalDate asOf) {
        projectAccessService.requireView(AgamottoSecurity.currentUserId(), projectId);
        LocalDate end = asOf != null ? asOf : LocalDate.now();
        return dashboardMapper.toResponse(
                dashboardService.generateReportForProject(projectId, period, end));
    }
}
