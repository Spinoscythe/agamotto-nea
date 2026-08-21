package com.srikrishnanethi.agamotto.service;

import com.srikrishnanethi.agamotto.entities.ProjectInvite;
import com.srikrishnanethi.agamotto.entities.ProjectMember;
import com.srikrishnanethi.agamotto.entities.enums.ProjectRole;

import java.util.List;

public interface CollaborationService {
    List<ProjectMember> listMembers(String projectId);

    List<ProjectInvite> listPendingInvites(String projectId);

    ProjectInvite invite(String projectId, String inviteeEmail, ProjectRole role);

    void cancelInvite(String projectId, String inviteeId);

    ProjectInvite acceptInvite(String inviteId);

    ProjectInvite declineInvite(String inviteId);

    ProjectMember updateMemberRole(String projectId, String userId, ProjectRole role);

    void removeMember(String projectId, String userId);

    void leaveProject(String projectId);
}
