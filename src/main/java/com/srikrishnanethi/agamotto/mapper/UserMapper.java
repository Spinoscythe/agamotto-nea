package com.srikrishnanethi.agamotto.mapper;

import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import com.srikrishnanethi.agamotto.dto.request.RegisterUserRequest;
import com.srikrishnanethi.agamotto.dto.response.AuthResponse;
import com.srikrishnanethi.agamotto.dto.response.UserProfileResponse;
import com.srikrishnanethi.agamotto.dto.response.UserResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;

@Component
public class UserMapper {

	/**
	 * Builds a transient {@link User} (+ default profile) from a register request.
	 * Password hashing is the service's responsibility — {@code passwordHash} is left null.
	 */
	public User toEntity(RegisterUserRequest request) {
		User user = new User();
		user.setFullName(request.fullName().trim());
		user.setEmail(request.email().trim().toLowerCase());
		user.setCreatedAt(Instant.now());

		UserProfile profile = new UserProfile();
		profile.setPreferredStart(LocalTime.of(9, 0));
		profile.setPreferredEnd(LocalTime.of(17, 0));
		profile.setIncludeWeekends(true);
		profile.setWeightPriority(1.0);
		profile.setWeightUrgency(1.0);
		profile.setWeightDuration(1.0);
		profile.setUpdatedAt(Instant.now());
		user.setProfile(profile);
		return user;
	}

	public UserResponse toResponse(User user) {
		UserProfile profile = user.getProfile();
		return new UserResponse(
				user.getId(),
				user.getFullName(),
				user.getEmail(),
				user.getCreatedAt(),
				profile == null ? null : toProfileResponse(profile));
	}

	public UserProfileResponse toProfileResponse(UserProfile profile) {
		return new UserProfileResponse(
				profile.getId(),
				profile.getPreferredStart(),
				profile.getPreferredEnd(),
				profile.isIncludeWeekends(),
				profile.getWeightPriority(),
				profile.getWeightUrgency(),
				profile.getWeightDuration(),
				profile.getUpdatedAt());
	}

	public AuthResponse toAuthResponse(String token, User user) {
		return new AuthResponse(token, toResponse(user));
	}
}
