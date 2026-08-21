package com.srikrishnanethi.agamotto.service.realtime;

/**
 * Publishes collaboration events to project WebSocket topics.
 */
public interface ProjectEventPublisher {

	void publishTaskChanged(String projectId, String actorUserId);

	void publishScheduleChanged(String projectId, String actorUserId);

	void publishMembersChanged(String projectId, String actorUserId);

	void publishPresence(String projectId, String userId, String displayName, boolean joined);
}
