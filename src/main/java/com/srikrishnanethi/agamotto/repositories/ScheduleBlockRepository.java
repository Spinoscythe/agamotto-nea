package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, String> {

	List<ScheduleBlock> findByScheduleIdOrderByStartTime(String scheduleId);

	/**
	 * Blocks for the user's projects whose parent plan was generated in {@code [from, to)}.
	 */
	@Query("""
			select b from ScheduleBlock b
			join b.schedule s
			join s.project p
			where p.owner.id = :userId
			  and s.generatedAt >= :from
			  and s.generatedAt < :to
			""")
	List<ScheduleBlock> findByOwnerIdAndPlanGeneratedAtBetween(
			@Param("userId") String userId,
			@Param("from") Instant from,
			@Param("to") Instant to);

	@Query("""
			select b from ScheduleBlock b
			join b.schedule s
			where s.project.id = :projectId
			  and s.generatedAt >= :from
			  and s.generatedAt < :to
			""")
	List<ScheduleBlock> findByProjectIdAndPlanGeneratedAtBetween(
			@Param("projectId") String projectId,
			@Param("from") Instant from,
			@Param("to") Instant to);
}
