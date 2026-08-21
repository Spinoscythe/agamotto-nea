package com.srikrishnanethi.agamotto.dto.request;

import com.srikrishnanethi.agamotto.entities.enums.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInviteRequest(
		@NotBlank @Email String email,
		@NotNull ProjectRole role) {
}
