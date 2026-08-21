package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.response.DashboardReportResponse;
import com.srikrishnanethi.agamotto.entities.enums.ReportPeriod;
import com.srikrishnanethi.agamotto.mapper.DashboardMapper;
import com.srikrishnanethi.agamotto.security.AgamottoSecurity;
import com.srikrishnanethi.agamotto.service.DashboardService;
import com.srikrishnanethi.agamotto.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final DashboardService dashboardService;
    private final DashboardMapper dashboardMapper;
    private final ProjectService projectService;

    public DashboardController(
            DashboardService dashboardService,
            DashboardMapper dashboardMapper,
            ProjectService projectService) {
        this.dashboardService = dashboardService;
        this.dashboardMapper = dashboardMapper;
        this.projectService = projectService;
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
        AgamottoSecurity.requireOwner(projectService.getById(projectId));
        LocalDate end = asOf != null ? asOf : LocalDate.now();
        return dashboardMapper.toResponse(
                dashboardService.generateReportForProject(projectId, period, end));
    }
}
