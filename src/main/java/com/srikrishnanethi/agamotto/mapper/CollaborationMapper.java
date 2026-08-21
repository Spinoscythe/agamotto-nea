package com.srikrishnanethi.agamotto.mapper;

import com.srikrishnanethi.agamotto.dto.response.ProjectInviteResponse;
import com.srikrishnanethi.agamotto.dto.response.ProjectMemberResponse;
import com.srikrishnanethi.agamotto.entities.ProjectInvite;
import com.srikrishnanethi.agamotto.entities.ProjectMember;
import org.springframework.stereotype.Component;

@Component
public class CollaborationMapper {

	public ProjectMemberResponse toMemberResponse(ProjectMember member) {
		return new ProjectMemberResponse(
				member.getId(),
				member.getProject().getId(),
				member.getUser().getId(),
				member.getUser().getEmail(),
				member.getUser().getFullName(),
				member.getRole(),
				member.getJoinedAt());
	}

	public ProjectInviteResponse toInviteResponse(ProjectInvite invite) {
		return new ProjectInviteResponse(
				invite.getId(),
				invite.getProject().getId(),
				invite.getProject().getName(),
				invite.getInviter().getId(),
				invite.getInviter().getFullName(),
				invite.getInviteeEmail(),
				invite.getRole(),
				invite.getStatus(),
				invite.getCreatedAt(),
				invite.getResolvedAt());
	}
}
