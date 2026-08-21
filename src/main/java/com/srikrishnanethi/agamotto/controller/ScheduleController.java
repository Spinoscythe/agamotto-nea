package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.request.GenerateScheduleRequest;
import com.srikrishnanethi.agamotto.dto.request.OverrideBlockRequest;
import com.srikrishnanethi.agamotto.dto.request.RescheduleBlockRequest;
import com.srikrishnanethi.agamotto.dto.response.RescheduleResponse;
import com.srikrishnanethi.agamotto.dto.response.ScheduleBlockResponse;
import com.srikrishnanethi.agamotto.dto.response.SchedulePlanResponse;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.mapper.ScheduleMapper;
import com.srikrishnanethi.agamotto.security.AgamottoSecurity;
import com.srikrishnanethi.agamotto.service.ProjectService;
import com.srikrishnanethi.agamotto.service.SchedulePlanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ScheduleController {

	private final SchedulePlanService schedulePlanService;
	private final ScheduleMapper scheduleMapper;
	private final ProjectService projectService;

	public ScheduleController(
			SchedulePlanService schedulePlanService,
			ScheduleMapper scheduleMapper,
			ProjectService projectService) {
		this.schedulePlanService = schedulePlanService;
		this.scheduleMapper = scheduleMapper;
		this.projectService = projectService;
	}

	@PostMapping({"/projects/{projectId}/schedules", "/projects/{projectId}/schedule"})
	@ResponseStatus(HttpStatus.CREATED)
	public SchedulePlanResponse generate(
			@PathVariable String projectId,
			@Valid @RequestBody GenerateScheduleRequest request) {
		AgamottoSecurity.requireOwner(projectService.getById(projectId));
		return scheduleMapper.toPlanResponse(
				schedulePlanService.generateAndPersist(projectId, request.startDate(), request.endDate()));
	}

	@GetMapping("/projects/{projectId}/schedules")
	public List<SchedulePlanResponse> listByProject(@PathVariable String projectId) {
		AgamottoSecurity.requireOwner(projectService.getById(projectId));
		return schedulePlanService.listByProject(projectId).stream()
				.map(scheduleMapper::toPlanResponse)
				.toList();
	}

	@GetMapping("/schedules/{planId}")
	public SchedulePlanResponse getPlan(@PathVariable String planId) {
		var plan = schedulePlanService.getById(planId);
		AgamottoSecurity.requireOwner(plan);
		return scheduleMapper.toPlanResponse(plan);
	}

	@PatchMapping("/schedules/{planId}/blocks/{blockId}")
	public ScheduleBlockResponse overrideBlockOnPlan(
			@PathVariable String planId,
			@PathVariable String blockId,
			@Valid @RequestBody OverrideBlockRequest request) {
		var block = schedulePlanService.getBlockById(blockId);
		AgamottoSecurity.requireOwner(block);
		if (!planId.equals(block.getSchedule().getId())) {
			throw new ResourceNotFoundException(
					"ScheduleBlock not found on plan " + planId + ": " + blockId);
		}
		return applyOverride(blockId, request);
	}

	@PatchMapping("/schedule-blocks/{blockId}")
	public ScheduleBlockResponse overrideBlock(
			@PathVariable String blockId,
			@Valid @RequestBody OverrideBlockRequest request) {
		AgamottoSecurity.requireOwner(schedulePlanService.getBlockById(blockId));
		return applyOverride(blockId, request);
	}

	@PostMapping("/schedule-blocks/{blockId}/reschedule")
	public RescheduleResponse reschedule(
			@PathVariable String blockId,
			@Valid @RequestBody RescheduleBlockRequest request) {
		AgamottoSecurity.requireOwner(schedulePlanService.getBlockById(blockId));
		return scheduleMapper.toRescheduleResponse(schedulePlanService.rescheduleBlock(
				blockId,
				request.startTime(),
				request.endTime(),
				request.reason()));
	}

	private ScheduleBlockResponse applyOverride(String blockId, OverrideBlockRequest request) {
		return scheduleMapper.toBlockResponse(schedulePlanService.overrideBlock(
				blockId,
				request.startTime(),
				request.endTime(),
				request.decision(),
				request.reason()));
	}
}
