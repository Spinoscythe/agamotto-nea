package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.SchedulePlan;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * True if the project has any plan row. Delete-project refuses while this
     * is true so generated timetables are not silently dropped.
     */
    boolean existsByProjectId(String projectId);
}
