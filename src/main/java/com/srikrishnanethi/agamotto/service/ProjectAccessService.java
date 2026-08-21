package com.srikrishnanethi.agamotto.service;

import com.srikrishnanethi.agamotto.entities.enums.ProjectRole;

import java.util.Optional;

/**
 * Resolves a caller's collaboration role on a project and enforces access gates.
 */
public interface ProjectAccessService {

	Optional<ProjectRole> findRole(String userId, String projectId);

	ProjectRole requireView(String userId, String projectId);

	ProjectRole requireEdit(String userId, String projectId);

	ProjectRole requireOwner(String userId, String projectId);

	boolean canAccess(String userId, String projectId);
}
