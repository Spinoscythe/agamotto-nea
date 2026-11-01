package com.srikrishnanethi.agamotto.dto.request;

import com.srikrishnanethi.agamotto.entities.enums.LeaveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateLeaveRequest(
        @NotBlank @Size(max = 120) String title,
        @NotNull LeaveType type,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @Size(max = 1000) String reason) {
}

