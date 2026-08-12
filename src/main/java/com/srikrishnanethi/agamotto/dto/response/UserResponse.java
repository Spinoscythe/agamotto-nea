package com.srikrishnanethi.agamotto.dto.response;

import java.time.Instant;

/**
 * Public user view — never includes {@code passwordHash}.
 */
public record UserResponse(
		String id,
		String fullName,
		String email,
		Instant createdAt,
		UserProfileResponse profile) {
}
