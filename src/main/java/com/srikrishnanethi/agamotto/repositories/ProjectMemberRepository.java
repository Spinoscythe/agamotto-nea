package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Legacy membership table access retained only for FK-safe cleanup when a
 * project is deleted. Project access is now owner-only.
 */
@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, String> {

    /**
     * Remove every membership for a project. Called immediately before the
     * project row is deleted.
     */
    void deleteByProjectId(String projectId);
}
