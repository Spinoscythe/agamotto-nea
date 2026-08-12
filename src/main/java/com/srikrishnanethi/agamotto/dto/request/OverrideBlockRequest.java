package com.srikrishnanethi.agamotto.dto.request;

import com.srikrishnanethi.agamotto.entities.enums.BlockDecision;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record OverrideBlockRequest(
		LocalDateTime startTime,
		LocalDateTime endTime,
		BlockDecision decision,
		@Size(max = 1000) String reason) {
}
