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
import com.srikrishnanethi.agamotto.service.ProjectAccessService;
import com.srikrishnanethi.agamotto.service.SchedulePlanService;
import com.srikrishnanethi.agamotto.service.realtime.ProjectEventPublisher;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ScheduleController {

	private final SchedulePlanService schedulePlanService;
	private final ScheduleMapper scheduleMapper;
	private final ProjectAccessService projectAccessService;
	private final ProjectEventPublisher projectEventPublisher;

	public ScheduleController(
			SchedulePlanService schedulePlanService,
			ScheduleMapper scheduleMapper,
			ProjectAccessService projectAccessService,
			ProjectEventPublisher projectEventPublisher) {
		this.schedulePlanService = schedulePlanService;
		this.scheduleMapper = scheduleMapper;
		this.projectAccessService = projectAccessService;
		this.projectEventPublisher = projectEventPublisher;
	}

	@PostMapping({"/projects/{projectId}/schedules", "/projects/{projectId}/schedule"})
	@ResponseStatus(HttpStatus.CREATED)
	public SchedulePlanResponse generate(
			@PathVariable String projectId,
			@Valid @RequestBody GenerateScheduleRequest request) {
		String actorId = AgamottoSecurity.currentUserId();
		projectAccessService.requireEdit(actorId, projectId);
		SchedulePlanResponse plan = scheduleMapper.toPlanResponse(
				schedulePlanService.generateAndPersist(projectId, request.startDate(), request.endDate()));
		projectEventPublisher.publishScheduleChanged(projectId, actorId);
		return plan;
	}

	@GetMapping("/projects/{projectId}/schedules")
	public List<SchedulePlanResponse> listByProject(@PathVariable String projectId) {
		projectAccessService.requireView(AgamottoSecurity.currentUserId(), projectId);
		return schedulePlanService.listByProject(projectId).stream()
				.map(scheduleMapper::toPlanResponse)
				.toList();
	}

	@GetMapping("/schedules/{planId}")
	public SchedulePlanResponse getPlan(@PathVariable String planId) {
		var plan = schedulePlanService.getById(planId);
		projectAccessService.requireView(AgamottoSecurity.currentUserId(), plan.getProject().getId());
		return scheduleMapper.toPlanResponse(plan);
	}

	@PatchMapping("/schedules/{planId}/blocks/{blockId}")
	public ScheduleBlockResponse overrideBlockOnPlan(
			@PathVariable String planId,
			@PathVariable String blockId,
			@Valid @RequestBody OverrideBlockRequest request) {
		var block = schedulePlanService.getBlockById(blockId);
		String projectId = block.getSchedule().getProject().getId();
		projectAccessService.requireEdit(AgamottoSecurity.currentUserId(), projectId);
		if (!planId.equals(block.getSchedule().getId())) {
			throw new ResourceNotFoundException(
					"ScheduleBlock not found on plan " + planId + ": " + blockId);
		}
		ScheduleBlockResponse updated = applyOverride(blockId, request);
		projectEventPublisher.publishScheduleChanged(projectId, AgamottoSecurity.currentUserId());
		return updated;
	}

	@PatchMapping("/schedule-blocks/{blockId}")
	public ScheduleBlockResponse overrideBlock(
			@PathVariable String blockId,
			@Valid @RequestBody OverrideBlockRequest request) {
		var block = schedulePlanService.getBlockById(blockId);
		String projectId = block.getSchedule().getProject().getId();
		projectAccessService.requireEdit(AgamottoSecurity.currentUserId(), projectId);
		ScheduleBlockResponse updated = applyOverride(blockId, request);
		projectEventPublisher.publishScheduleChanged(projectId, AgamottoSecurity.currentUserId());
		return updated;
	}

	@PostMapping("/schedule-blocks/{blockId}/reschedule")
	public RescheduleResponse reschedule(
			@PathVariable String blockId,
			@Valid @RequestBody RescheduleBlockRequest request) {
		var block = schedulePlanService.getBlockById(blockId);
		String projectId = block.getSchedule().getProject().getId();
		projectAccessService.requireEdit(AgamottoSecurity.currentUserId(), projectId);
		RescheduleResponse response = scheduleMapper.toRescheduleResponse(schedulePlanService.rescheduleBlock(
				blockId,
				request.startTime(),
				request.endTime(),
				request.reason()));
		projectEventPublisher.publishScheduleChanged(projectId, AgamottoSecurity.currentUserId());
		return response;
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
