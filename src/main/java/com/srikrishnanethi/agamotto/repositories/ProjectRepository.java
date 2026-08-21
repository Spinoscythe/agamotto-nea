package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CRUD for {@code projects}. Ownership queries use {@code user_id} (the owner);
 * membership queries live on {@link ProjectMemberRepository}.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    /**
     * Every project this user created (not including projects they were invited to).
     * {@code ProjectServiceImpl.listAccessible} unions this with memberships.
     */
    List<Project> findByOwnerId(String userId);
}
