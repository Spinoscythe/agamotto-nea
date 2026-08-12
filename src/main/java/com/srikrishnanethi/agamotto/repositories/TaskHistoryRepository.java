package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, String> {

    List<TaskHistory> findByTaskIdOrderByChangedAtDesc(String taskId);
}
