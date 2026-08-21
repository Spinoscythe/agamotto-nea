package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.dto.response.NotificationResponse;
import com.srikrishnanethi.agamotto.mapper.NotificationMapper;
import com.srikrishnanethi.agamotto.security.AgamottoSecurity;
import com.srikrishnanethi.agamotto.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;
	private final NotificationMapper notificationMapper;

	public NotificationController(
			NotificationService notificationService,
			NotificationMapper notificationMapper) {
		this.notificationService = notificationService;
		this.notificationMapper = notificationMapper;
	}

	@GetMapping
	public List<NotificationResponse> listUnread(@RequestParam(required = false) String userId) {
		String me = AgamottoSecurity.currentUserId();
		AgamottoSecurity.requireSelf(userId);
		return notificationService.listUnread(me).stream()
				.map(notificationMapper::toResponse)
				.toList();
	}

	@PostMapping("/{notificationId}/read")
	public NotificationResponse markRead(@PathVariable String notificationId) {
		return notificationMapper.toResponse(
				notificationService.markRead(notificationId, AgamottoSecurity.currentUserId()));
	}
}
