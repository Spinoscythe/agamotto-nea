package com.srikrishnanethi.agamotto.dto.response;

import com.srikrishnanethi.agamotto.entities.enums.InviteStatus;
import com.srikrishnanethi.agamotto.entities.enums.ProjectRole;

import java.time.Instant;

public record ProjectInviteResponse(
		String id,
		String projectId,
		String projectName,
		String inviterId,
		String inviterName,
		String inviteeEmail,
		ProjectRole role,
		InviteStatus status,
		Instant createdAt,
		Instant resolvedAt) {
}
