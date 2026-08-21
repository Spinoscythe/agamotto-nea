package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Membership lookups for {@code project_members}. The unique pair
 * {@code (project_id, user_id)} is the natural key for access checks.
 */
@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, String> {

    /**
     * Access-check primitive: is this user on this project, and with which role?
     */
    Optional<ProjectMember> findByProjectIdAndUserId(String projectId, String userId);

    /**
     * Every member of a project, oldest join first. Share-panel roster.
     */
    List<ProjectMember> findByProjectIdOrderByJoinedAtAsc(String projectId);

    /**
     * Every project this user belongs to (owned or invited). Combined with
     * {@code ProjectRepository.findByOwnerId} in {@code listAccessible}.
     */
    List<ProjectMember> findByUserId(String userId);

    /**
     * True if the pair already exists. Used before inserting so we do not trip
     * the unique constraint with a 500.
     */
    boolean existsByProjectIdAndUserId(String projectId, String userId);

    /**
     * Remove every membership for a project. Called immediately before the
     * project row is deleted.
     */
    void deleteByProjectId(String projectId);
}
