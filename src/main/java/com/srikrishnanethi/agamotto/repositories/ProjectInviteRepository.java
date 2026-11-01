package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.ProjectInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Legacy invite table access retained only for FK-safe cleanup when a project
 * is deleted. Group collaboration features are no longer exposed.
 */
@Repository
public interface ProjectInviteRepository extends JpaRepository<ProjectInvite, String> {

    /**
     * Remove every invite for a project. Called immediately before the project
     * row is deleted.
     */
    void deleteByProjectId(String projectId);
}
