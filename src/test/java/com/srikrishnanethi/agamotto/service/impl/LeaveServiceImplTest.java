package com.srikrishnanethi.agamotto.service.impl;

import com.srikrishnanethi.agamotto.dto.request.CreateLeaveRequest;
import com.srikrishnanethi.agamotto.dto.request.UpdateLeaveRequest;
import com.srikrishnanethi.agamotto.entities.Leave;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.enums.LeaveType;
import com.srikrishnanethi.agamotto.exception.ForbiddenException;
import com.srikrishnanethi.agamotto.exception.ResourceNotFoundException;
import com.srikrishnanethi.agamotto.mapper.LeaveMapper;
import com.srikrishnanethi.agamotto.repositories.LeaveRepository;
import com.srikrishnanethi.agamotto.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceImplTest {

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private UserRepository userRepository;

    private LeaveMapper leaveMapper;
    private LeaveServiceImpl service;

    @BeforeEach
    void setUp() {
        leaveMapper = new LeaveMapper();
        service = new LeaveServiceImpl(leaveRepository, userRepository, leaveMapper);
    }

    @Test
    void createLeaveSuccessfullyForUser() {
        User user = new User();
        user.setId("user-1");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(leaveRepository.save(any(Leave.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 1, 17, 0);
        CreateLeaveRequest request = new CreateLeaveRequest("Dentist", LeaveType.SHORT_BREAK, start, end, "Appointment");

        Leave result = service.createLeave("user-1", request);

        assertNotNull(result);
        assertEquals("Dentist", result.getTitle());
        assertEquals(LeaveType.SHORT_BREAK, result.getType());
        assertEquals(user, result.getUser());
        assertFalse(result.isGlobal());
        verify(leaveRepository).save(any(Leave.class));
    }

    @Test
    void createGlobalLeaveWithoutUser() {
        when(leaveRepository.save(any(Leave.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime start = LocalDateTime.of(2026, 12, 25, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 25, 23, 59);
        CreateLeaveRequest request = new CreateLeaveRequest("Christmas", LeaveType.HOLIDAY, start, end, "Bank holiday");

        Leave result = service.createLeave(null, request);

        assertNotNull(result);
        assertNull(result.getUser());
        assertTrue(result.isGlobal());
        verify(userRepository, never()).findById(any());
        verify(leaveRepository).save(any(Leave.class));
    }

    @Test
    void createLeaveRejectsInvalidTimeRange() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 17, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 1, 9, 0);
        CreateLeaveRequest request = new CreateLeaveRequest("Invalid", LeaveType.VACATION, start, end, null);

        assertThrows(IllegalArgumentException.class, () -> service.createLeave("user-1", request));
        verify(leaveRepository, never()).save(any());
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        when(leaveRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById("non-existent"));
    }

    @Test
    void updateLeavePreventsUnauthorizedUser() {
        User owner = new User();
        owner.setId("owner-id");

        Leave leave = new Leave(owner, "Vacation", LeaveType.VACATION,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), "Off");
        leave.setId("leave-1");

        when(leaveRepository.findById("leave-1")).thenReturn(Optional.of(leave));

        UpdateLeaveRequest request = new UpdateLeaveRequest("Hacked", null, null, null, null);

        assertThrows(ForbiddenException.class, () -> service.updateLeave("leave-1", "intruder-id", request));
        verify(leaveRepository, never()).save(any());
    }

    @Test
    void deleteLeaveRemovesWhenAuthorized() {
        User owner = new User();
        owner.setId("owner-id");

        Leave leave = new Leave(owner, "Vacation", LeaveType.VACATION,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), "Off");
        leave.setId("leave-1");

        when(leaveRepository.findById("leave-1")).thenReturn(Optional.of(leave));

        service.deleteLeave("leave-1", "owner-id");

        verify(leaveRepository).delete(leave);
    }
}

