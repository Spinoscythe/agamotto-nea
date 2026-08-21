package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.ProjectInvite;
import com.srikrishnanethi.agamotto.entities.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Invite lookups for {@code project_invites}. Pending-row uniqueness
 * {@code (project_id, invitee_email) WHERE status = PENDING} is enforced in
 * {@code CollaborationServiceImpl}, not by a partial unique index.
 */
@Repository
public interface ProjectInviteRepository extends JpaRepository<ProjectInvite, String> {

    /**
     * Invites for a project in one status, newest first. The share panel lists
     * {@code PENDING} rows this way.
     */
    List<ProjectInvite> findByProjectIdAndStatusOrderByCreatedAtDesc(String projectId,
                                                                    InviteStatus status);

    /**
     * Find a still-pending invite for this project + email so a duplicate send
     * can be refused with 409.
     */
    Optional<ProjectInvite> findByProjectIdAndInviteeEmailIgnoreCaseAndStatus(String projectId,
                                                                             String inviteeEmail,
                                                                             InviteStatus status);

    /**
     * Remove every invite for a project. Called immediately before the project
     * row is deleted.
     */
    void deleteByProjectId(String projectId);
}
