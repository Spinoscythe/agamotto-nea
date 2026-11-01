package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CRUD for {@code projects}. Projects are owned by a single user; access is
 * resolved from {@code user_id} only.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    /**
     * Every project this user created.
     * {@code ProjectServiceImpl.listAccessible} returns exactly this set.
     */
    List<Project> findByOwnerId(String userId);
}
