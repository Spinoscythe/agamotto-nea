package com.srikrishnanethi.agamotto.dto.response;

/**
 * Login/register payload: JWT for the SPA plus the public user view.
 */
public record AuthResponse(
		String token,
		String tokenType,
		UserResponse user) {

	public AuthResponse(String token, UserResponse user) {
		this(token, "Bearer", user);
	}
}
