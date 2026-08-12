package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.SchedulePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchedulePlanRepository extends JpaRepository<SchedulePlan, String> {

	List<SchedulePlan> findByProjectIdOrderByGeneratedAtDesc(String projectId);

	boolean existsByProjectId(String projectId);
}
