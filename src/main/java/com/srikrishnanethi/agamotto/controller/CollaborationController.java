package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.request.CreateInviteRequest;
import com.srikrishnanethi.agamotto.dto.request.UpdateMemberRoleRequest;
import com.srikrishnanethi.agamotto.dto.response.ProjectInviteResponse;
import com.srikrishnanethi.agamotto.dto.response.ProjectMemberResponse;
import com.srikrishnanethi.agamotto.mapper.CollaborationMapper;
import com.srikrishnanethi.agamotto.service.CollaborationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CollaborationController {

	private final CollaborationService collaborationService;
	private final CollaborationMapper collaborationMapper;

	public CollaborationController(
			CollaborationService collaborationService,
			CollaborationMapper collaborationMapper) {
		this.collaborationService = collaborationService;
		this.collaborationMapper = collaborationMapper;
	}

	@GetMapping("/projects/{projectId}/members")
	public List<ProjectMemberResponse> listMembers(@PathVariable String projectId) {
		return collaborationService.listMembers(projectId).stream()
				.map(collaborationMapper::toMemberResponse)
				.toList();
	}

	@GetMapping("/projects/{projectId}/invites")
	public List<ProjectInviteResponse> listInvites(@PathVariable String projectId) {
		return collaborationService.listPendingInvites(projectId).stream()
				.map(collaborationMapper::toInviteResponse)
				.toList();
	}

	@PostMapping("/projects/{projectId}/invites")
	@ResponseStatus(HttpStatus.CREATED)
	public ProjectInviteResponse invite(
			@PathVariable String projectId,
			@Valid @RequestBody CreateInviteRequest request) {
		return collaborationMapper.toInviteResponse(
				collaborationService.invite(projectId, request.email(), request.role()));
	}

	@DeleteMapping("/projects/{projectId}/invites/{inviteId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancelInvite(@PathVariable String projectId, @PathVariable String inviteId) {
		collaborationService.cancelInvite(projectId, inviteId);
	}

	@PatchMapping("/projects/{projectId}/members/{userId}")
	public ProjectMemberResponse updateRole(
			@PathVariable String projectId,
			@PathVariable String userId,
			@Valid @RequestBody UpdateMemberRoleRequest request) {
		return collaborationMapper.toMemberResponse(
				collaborationService.updateMemberRole(projectId, userId, request.role()));
	}

	@DeleteMapping("/projects/{projectId}/members/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeMember(@PathVariable String projectId, @PathVariable String userId) {
		collaborationService.removeMember(projectId, userId);
	}

	@PostMapping("/projects/{projectId}/leave")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void leave(@PathVariable String projectId) {
		collaborationService.leaveProject(projectId);
	}

	@PostMapping("/invites/{inviteId}/accept")
	public ProjectInviteResponse accept(@PathVariable String inviteId) {
		return collaborationMapper.toInviteResponse(collaborationService.acceptInvite(inviteId));
	}

	@PostMapping("/invites/{inviteId}/decline")
	public ProjectInviteResponse decline(@PathVariable String inviteId) {
		return collaborationMapper.toInviteResponse(collaborationService.declineInvite(inviteId));
	}
}
