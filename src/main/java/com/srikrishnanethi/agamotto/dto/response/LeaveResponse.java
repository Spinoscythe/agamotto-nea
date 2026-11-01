package com.srikrishnanethi.agamotto.dto.response;

import com.srikrishnanethi.agamotto.entities.enums.LeaveType;

import java.time.Instant;
import java.time.LocalDateTime;

public record LeaveResponse(
        String id,
        String userId,
        String title,
        LeaveType type,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String reason,
        Instant createdAt,
        boolean isGlobal) {
}

