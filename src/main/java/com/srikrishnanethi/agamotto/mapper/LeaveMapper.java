package com.srikrishnanethi.agamotto.mapper;

import com.srikrishnanethi.agamotto.dto.request.CreateLeaveRequest;
import com.srikrishnanethi.agamotto.dto.response.LeaveResponse;
import com.srikrishnanethi.agamotto.entities.Leave;
import com.srikrishnanethi.agamotto.entities.User;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class LeaveMapper {

    public Leave toEntity(CreateLeaveRequest request, User user) {
        Leave leave = new Leave();
        leave.setUser(user);
        leave.setTitle(request.title().trim());
        leave.setType(request.type());
        leave.setStartTime(request.startTime());
        leave.setEndTime(request.endTime());
        leave.setReason(request.reason() != null ? request.reason().trim() : null);
        leave.setCreatedAt(Instant.now());
        return leave;
    }

    public LeaveResponse toResponse(Leave leave) {
        return new LeaveResponse(
                leave.getId(),
                leave.getUser() != null ? leave.getUser().getId() : null,
                leave.getTitle(),
                leave.getType(),
                leave.getStartTime(),
                leave.getEndTime(),
                leave.getReason(),
                leave.getCreatedAt(),
                leave.isGlobal());
    }
}

