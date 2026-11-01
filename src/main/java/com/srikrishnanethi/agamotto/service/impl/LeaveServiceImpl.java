package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.dto.request.CreateLeaveRequest;
import com.srikrishnanethi.agamotto.dto.request.UpdateLeaveRequest;
import com.srikrishnanethi.agamotto.entities.Leave;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.exception.ForbiddenException;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.mapper.LeaveMapper;
import com.srikrishnanethi.agamotto.repositories.LeaveRepository;
import com.srikrishnanethi.agamotto.repositories.UserRepository;
import com.srikrishnanethi.agamotto.service.LeaveService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRepository leaveRepository;
    private final UserRepository userRepository;
    private final LeaveMapper leaveMapper;

    public LeaveServiceImpl(
            LeaveRepository leaveRepository,
            UserRepository userRepository,
            LeaveMapper leaveMapper) {
        this.leaveRepository = leaveRepository;
        this.userRepository = userRepository;
        this.leaveMapper = leaveMapper;
    }

    @Override
    public Leave createLeave(String userId, CreateLeaveRequest request) {
        Objects.requireNonNull(request, "request");
        validateTimes(request.startTime(), request.endTime());

        User user = null;
        if (userId != null && !userId.isBlank()) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        }

        Leave leave = leaveMapper.toEntity(request, user);
        return leaveRepository.save(leave);
    }

    @Override
    @Transactional(readOnly = true)
    public Leave getById(String leaveId) {
        return leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found: " + leaveId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Leave> getUserLeaves(String userId) {
        if (userId == null || userId.isBlank()) {
            return leaveRepository.findByUserIsNullOrderByStartTimeAsc();
        }
        return leaveRepository.findByUserIdOrderByStartTimeAsc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Leave> getActiveLeavesAndHolidays(String userId, LocalDateTime start, LocalDateTime end) {
        validateTimes(start, end);
        if (userId == null || userId.isBlank()) {
            return leaveRepository.findAllLeavesInWindow(start, end);
        }
        return leaveRepository.findActiveLeavesAndHolidays(userId, start, end);
    }

    @Override
    public Leave updateLeave(String leaveId, String actorUserId, UpdateLeaveRequest request) {
        Objects.requireNonNull(request, "request");
        Leave leave = getById(leaveId);
        assertUserCanModify(leave, actorUserId);

        LocalDateTime newStart = request.startTime() != null ? request.startTime() : leave.getStartTime();
        LocalDateTime newEnd = request.endTime() != null ? request.endTime() : leave.getEndTime();
        validateTimes(newStart, newEnd);

        if (request.title() != null && !request.title().isBlank()) {
            leave.setTitle(request.title().trim());
        }
        if (request.type() != null) {
            leave.setType(request.type());
        }
        leave.setStartTime(newStart);
        leave.setEndTime(newEnd);
        if (request.reason() != null) {
            leave.setReason(request.reason().trim());
        }

        return leaveRepository.save(leave);
    }

    @Override
    public void deleteLeave(String leaveId, String actorUserId) {
        Leave leave = getById(leaveId);
        assertUserCanModify(leave, actorUserId);
        leaveRepository.delete(leave);
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
    }

    private void assertUserCanModify(Leave leave, String actorUserId) {
        if (leave.getUser() != null && actorUserId != null && !leave.getUser().getId().equals(actorUserId)) {
            throw new ForbiddenException("You cannot modify another user's leave");
        }
    }
}

