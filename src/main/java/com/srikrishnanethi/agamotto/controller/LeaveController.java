package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.request.CreateLeaveRequest;
import com.srikrishnanethi.agamotto.dto.request.UpdateLeaveRequest;
import com.srikrishnanethi.agamotto.dto.response.LeaveResponse;
import com.srikrishnanethi.agamotto.entities.Leave;
import com.srikrishnanethi.agamotto.mapper.LeaveMapper;
import com.srikrishnanethi.agamotto.security.AgamottoSecurity;
import com.srikrishnanethi.agamotto.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class LeaveController {

    private final LeaveService leaveService;
    private final LeaveMapper leaveMapper;

    public LeaveController(LeaveService leaveService, LeaveMapper leaveMapper) {
        this.leaveService = leaveService;
        this.leaveMapper = leaveMapper;
    }

    @PostMapping("/leaves")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveResponse createLeave(@Valid @RequestBody CreateLeaveRequest request) {
        String actorId = AgamottoSecurity.currentUserId();
        Leave leave = leaveService.createLeave(actorId, request);
        return leaveMapper.toResponse(leave);
    }

    @GetMapping("/leaves")
    public List<LeaveResponse> listLeaves(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        String actorId = AgamottoSecurity.currentUserId();
        List<Leave> leaves;
        if (start != null && end != null) {
            leaves = leaveService.getActiveLeavesAndHolidays(actorId, start, end);
        } else {
            leaves = leaveService.getUserLeaves(actorId);
        }
        return leaves.stream().map(leaveMapper::toResponse).toList();
    }

    @GetMapping("/leaves/{id}")
    public LeaveResponse getLeaveById(@PathVariable String id) {
        Leave leave = leaveService.getById(id);
        if (leave.getUser() != null) {
            AgamottoSecurity.requireSelf(leave.getUser().getId());
        }
        return leaveMapper.toResponse(leave);
    }

    @PatchMapping("/leaves/{id}")
    public LeaveResponse updateLeave(
            @PathVariable String id,
            @Valid @RequestBody UpdateLeaveRequest request) {
        String actorId = AgamottoSecurity.currentUserId();
        Leave updated = leaveService.updateLeave(id, actorId, request);
        return leaveMapper.toResponse(updated);
    }

    @DeleteMapping("/leaves/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLeave(@PathVariable String id) {
        String actorId = AgamottoSecurity.currentUserId();
        leaveService.deleteLeave(id, actorId);
    }

    @PostMapping("/users/{userId}/leaves")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveResponse createUserLeave(
            @PathVariable String userId,
            @Valid @RequestBody CreateLeaveRequest request) {
        AgamottoSecurity.requireSelf(userId);
        Leave leave = leaveService.createLeave(userId, request);
        return leaveMapper.toResponse(leave);
    }

    @GetMapping("/users/{userId}/leaves")
    public List<LeaveResponse> listUserLeaves(@PathVariable String userId) {
        AgamottoSecurity.requireSelf(userId);
        return leaveService.getUserLeaves(userId).stream()
                .map(leaveMapper::toResponse)
                .toList();
    }
}

