package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.request.CreateLeaveRequest;
import com.srikrishnanethi.agamotto.dto.request.UpdateLeaveRequest;
import com.srikrishnanethi.agamotto.dto.response.LeaveResponse;
import com.srikrishnanethi.agamotto.entities.Leave;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.entities.enums.LeaveType;
import com.srikrishnanethi.agamotto.mapper.LeaveMapper;
import com.srikrishnanethi.agamotto.service.LeaveService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveControllerTest {

    @Mock
    private LeaveService leaveService;

    private LeaveMapper leaveMapper;
    private LeaveController controller;

    @BeforeEach
    void setUp() {
        leaveMapper = new LeaveMapper();
        controller = new LeaveController(leaveService, leaveMapper);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-1", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createLeaveUsesAuthenticatedUser() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 1, 17, 0);
        CreateLeaveRequest request = new CreateLeaveRequest("Vacation", LeaveType.VACATION, start, end, "Trip");

        User user = new User();
        user.setId("user-1");
        Leave created = new Leave(user, "Vacation", LeaveType.VACATION, start, end, "Trip");
        created.setId("leave-1");

        when(leaveService.createLeave(eq("user-1"), eq(request))).thenReturn(created);

        LeaveResponse response = controller.createLeave(request);

        assertNotNull(response);
        assertEquals("leave-1", response.id());
        assertEquals("user-1", response.userId());
        assertEquals("Vacation", response.title());
        verify(leaveService).createLeave("user-1", request);
    }

    @Test
    void listLeavesReturnsUserLeaves() {
        User user = new User();
        user.setId("user-1");
        Leave leave = new Leave(user, "Doctor", LeaveType.PERSONAL,
                LocalDateTime.now(), LocalDateTime.now().plusHours(2), "Checkup");
        leave.setId("leave-1");

        when(leaveService.getUserLeaves("user-1")).thenReturn(List.of(leave));

        List<LeaveResponse> responses = controller.listLeaves(null, null);

        assertEquals(1, responses.size());
        assertEquals("leave-1", responses.get(0).id());
        assertEquals("Doctor", responses.get(0).title());
    }

    @Test
    void deleteLeaveDelegatesToService() {
        controller.deleteLeave("leave-1");
        verify(leaveService).deleteLeave("leave-1", "user-1");
    }

    @Test
    void updateLeaveDelegatesToService() {
        UpdateLeaveRequest request = new UpdateLeaveRequest("Updated", null, null, null, null);
        User user = new User();
        user.setId("user-1");
        Leave updated = new Leave(user, "Updated", LeaveType.PERSONAL,
                LocalDateTime.now(), LocalDateTime.now().plusHours(2), "Checkup");
        updated.setId("leave-1");

        when(leaveService.updateLeave("leave-1", "user-1", request)).thenReturn(updated);

        LeaveResponse response = controller.updateLeave("leave-1", request);
        assertEquals("Updated", response.title());
        verify(leaveService).updateLeave("leave-1", "user-1", request);
    }
}

