package com.srikrishnanethi.agamotto.service;

import com.srikrishnanethi.agamotto.dto.request.CreateLeaveRequest;
import com.srikrishnanethi.agamotto.dto.request.UpdateLeaveRequest;
import com.srikrishnanethi.agamotto.entities.Leave;

import java.time.LocalDateTime;
import java.util.List;

public interface LeaveService {

    Leave createLeave(String userId, CreateLeaveRequest request);

    Leave getById(String leaveId);

    List<Leave> getUserLeaves(String userId);

    List<Leave> getActiveLeavesAndHolidays(String userId, LocalDateTime start, LocalDateTime end);

    Leave updateLeave(String leaveId, String actorUserId, UpdateLeaveRequest request);

    void deleteLeave(String leaveId, String actorUserId);
}

