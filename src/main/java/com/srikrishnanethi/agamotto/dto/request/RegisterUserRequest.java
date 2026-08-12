package com.srikrishnanethi.agamotto.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
		@NotBlank @Size(max = 120) @JsonAlias("displayName") String fullName,
		@NotBlank @Email String email,
		@NotBlank @Size(min = 6, max = 100) String password) {
}
