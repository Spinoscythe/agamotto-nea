package com.srikrishnanethi.agamotto.dto.request;

import com.srikrishnanethi.agamotto.entities.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull ProjectRole role) {
}
