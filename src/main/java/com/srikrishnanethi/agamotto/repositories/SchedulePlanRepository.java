package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.SchedulePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CRUD for {@code schedule_plans}. A project may have many historical plans;
 * generating a new one archives previous {@code ACTIVE} rows.
 */
@Repository
public interface SchedulePlanRepository extends JpaRepository<SchedulePlan, String> {

    /**
     * Every plan for a project, newest generated first. Used by the schedules
     * page and by the engine when it archives older ACTIVE plans.
     */
    List<SchedulePlan> findByProjectIdOrderByGeneratedAtDesc(String projectId);

    /**
     * True if the project has any plan row.
     */
    boolean existsByProjectId(String projectId);

    /**
     * Remove every plan for a project. Called after the plans' blocks have
     * been deleted during project deletion.
     */
    @Modifying
    @Query("delete from SchedulePlan s where s.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") String projectId);
}
