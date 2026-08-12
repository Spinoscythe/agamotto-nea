package com.srikrishnanethi.agamotto.mapper;

import com.srikrishnanethi.agamotto.dto.response.RescheduleResponse;
import com.srikrishnanethi.agamotto.dto.response.ScheduleBlockResponse;
import com.srikrishnanethi.agamotto.dto.response.SchedulePlanResponse;
import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.SchedulePlan;
import com.srikrishnanethi.agamotto.service.scheduler.GeneratedSchedule;
import com.srikrishnanethi.agamotto.service.scheduler.RescheduleResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScheduleMapper {

	public ScheduleBlockResponse toBlockResponse(ScheduleBlock block) {
		return new ScheduleBlockResponse(
				block.getId(),
				block.getSchedule().getId(),
				block.getTask().getId(),
				block.getStartTime(),
				block.getEndTime(),
				block.getDecision(),
				block.getReason(),
				block.isManuallyOverridden());
	}

	public SchedulePlanResponse toPlanResponse(SchedulePlan plan) {
		return toPlanResponse(plan, null);
	}

	public SchedulePlanResponse toPlanResponse(GeneratedSchedule generated) {
		return toPlanResponse(generated.plan(), generated.explanationSummary());
	}

	public SchedulePlanResponse toPlanResponse(SchedulePlan plan, String explanationSummary) {
		List<ScheduleBlockResponse> blocks = plan.getBlocks().stream()
				.map(this::toBlockResponse)
				.toList();
		return new SchedulePlanResponse(
				plan.getId(),
				plan.getProject().getId(),
				plan.getMode(),
				plan.getStatus(),
				plan.getStartDate(),
				plan.getEndDate(),
				plan.getGeneratedAt(),
				explanationSummary,
				blocks);
	}

	public RescheduleResponse toRescheduleResponse(RescheduleResult result) {
		if (result.movedBlock() != null) {
			return new RescheduleResponse("MOVED", toBlockResponse(result.movedBlock()), null);
		}
		return new RescheduleResponse("REGENERATED", null, toPlanResponse(result.regenerated()));
	}
}
