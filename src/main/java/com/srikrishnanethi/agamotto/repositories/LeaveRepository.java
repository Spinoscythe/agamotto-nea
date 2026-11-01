package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persistence repository for user leaves, breaks, and global holidays.
 */
@Repository
public interface LeaveRepository extends JpaRepository<Leave, String> {

    List<Leave> findByUserIdOrderByStartTimeAsc(String userId);

    List<Leave> findByUserIsNullOrderByStartTimeAsc();

    @Query("SELECT l FROM Leave l " +
           "WHERE (l.user.id = :userId OR l.user IS NULL) " +
           "AND l.startTime < :windowEnd AND l.endTime > :windowStart " +
           "ORDER BY l.startTime ASC")
    List<Leave> findActiveLeavesAndHolidays(
            @Param("userId") String userId,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd);

    @Query("SELECT l FROM Leave l " +
           "WHERE l.user.id = :userId " +
           "AND l.startTime < :windowEnd AND l.endTime > :windowStart " +
           "ORDER BY l.startTime ASC")
    List<Leave> findUserLeavesInWindow(
            @Param("userId") String userId,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd);

    @Query("SELECT l FROM Leave l " +
           "WHERE l.startTime < :windowEnd AND l.endTime > :windowStart " +
           "ORDER BY l.startTime ASC")
    List<Leave> findAllLeavesInWindow(
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd);
}

