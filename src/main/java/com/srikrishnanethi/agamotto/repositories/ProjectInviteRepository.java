package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.ProjectInvite;
import com.srikrishnanethi.agamotto.entities.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectInviteRepository extends JpaRepository<ProjectInvite, String> {
    List<ProjectInvite> findByProjectIdAndStatusOrderByCreatedAtDesc(String projectId,
                                                                     InviteStatus status);

    Optional<ProjectInvite> findByProjectIdAndInviteeEmailIgnoreCaseAndStatus(String projectId,
                                                                              String inviteeEmail,
                                                                              InviteStatus status);

    void deleteByProjectId(String projectId);
}
