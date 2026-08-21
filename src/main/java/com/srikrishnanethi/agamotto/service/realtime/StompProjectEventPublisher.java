package com.srikrishnanethi.agamotto.service.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class StompProjectEventPublisher implements ProjectEventPublisher {

	public static final String TOPIC_PREFIX = "/topic/projects/";

	private final SimpMessagingTemplate messagingTemplate;

	public StompProjectEventPublisher(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@Override
	public void publishTaskChanged(String projectId, String actorUserId) {
		publish(projectId, "TASK_CHANGED", actorUserId, null, null);
	}

	@Override
	public void publishScheduleChanged(String projectId, String actorUserId) {
		publish(projectId, "SCHEDULE_CHANGED", actorUserId, null, null);
	}

	@Override
	public void publishMembersChanged(String projectId, String actorUserId) {
		publish(projectId, "MEMBERS_CHANGED", actorUserId, null, null);
	}

	@Override
	public void publishPresence(String projectId, String userId, String displayName, boolean joined) {
		publish(
				projectId,
				joined ? "PRESENCE_JOIN" : "PRESENCE_LEAVE",
				userId,
				displayName,
				null);
	}

	private void publish(
			String projectId,
			String type,
			String actorUserId,
			String displayName,
			String payload) {
		Map<String, Object> event = new LinkedHashMap<>();
		event.put("type", type);
		event.put("projectId", projectId);
		event.put("actorUserId", actorUserId);
		event.put("displayName", displayName);
		event.put("payload", payload);
		event.put("at", Instant.now().toString());
		messagingTemplate.convertAndSend(TOPIC_PREFIX + projectId, (Object) event);
	}
}
