package com.srikrishnanethi.agamotto.dto.response;

import com.srikrishnanethi.agamotto.entities.enums.ProjectRole;

import java.time.Instant;

public record ProjectMemberResponse(
		String id,
		String projectId,
		String userId,
		String email,
		String fullName,
		ProjectRole role,
		Instant joinedAt) {
}
