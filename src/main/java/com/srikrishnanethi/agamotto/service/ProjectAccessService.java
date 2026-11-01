package com.srikrishnanethi.agamotto.service;

/**
 * Enforces project ownership for project-scoped endpoints.
 */
public interface ProjectAccessService {

	void requireAccess(String userId, String projectId);

	void requireOwner(String userId, String projectId);

	boolean canAccess(String userId, String projectId);
}
