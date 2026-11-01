package com.srikrishnanethi.agamotto.dto.request;

import com.srikrishnanethi.agamotto.entities.enums.LeaveType;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateLeaveRequest(
        @Size(max = 120) String title,
        LeaveType type,
        LocalDateTime startTime,
        LocalDateTime endTime,
        @Size(max = 1000) String reason) {
}

