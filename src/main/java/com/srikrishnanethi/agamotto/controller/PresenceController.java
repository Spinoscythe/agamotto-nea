package com.srikrishnanethi.agamotto.controller;

import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.repositories.UserRepository;
import com.srikrishnanethi.agamotto.security.AgamottoSecurity;
import com.srikrishnanethi.agamotto.service.realtime.ProjectEventPublisher;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class PresenceController {

	private final ProjectEventPublisher projectEventPublisher;
	private final UserRepository userRepository;

	public PresenceController(ProjectEventPublisher projectEventPublisher, UserRepository userRepository) {
		this.projectEventPublisher = projectEventPublisher;
		this.userRepository = userRepository;
	}

	@MessageMapping("/projects/{projectId}/presence")
	public void presence(
			@DestinationVariable String projectId,
			Map<String, String> body,
			Principal principal) {
		String userId = principal != null ? principal.getName() : AgamottoSecurity.currentUserId();
		String action = body == null ? "join" : body.getOrDefault("action", "join");
		String displayName = userRepository.findById(userId)
				.map(User::getFullName)
				.orElse(userId);
		boolean joined = !"leave".equalsIgnoreCase(action);
		projectEventPublisher.publishPresence(projectId, userId, displayName, joined);
	}
}
