package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * CRUD for {@code schedule_blocks}. Sibling slots on one plan are loaded by
 * {@code schedule_id}; dashboard reports join through the parent plan's
 * {@code generated_at} window.
 */
@Repository
public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, String> {

    /**
     * Every block on a plan, earliest start first. Used after a reschedule to
     * re-check overlaps among siblings.
     */
    List<ScheduleBlock> findByScheduleIdOrderByStartTime(String scheduleId);

    /**
     * Blocks on plans owned by {@code userId} whose parent plan was generated
     * in {@code [from, to)}. User-level dashboard scheduled/delayed/excluded counts.
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

    /**
     * Blocks on plans for one project whose parent plan was generated in
     * {@code [from, to)}. Project-level dashboard counts.
     */
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
