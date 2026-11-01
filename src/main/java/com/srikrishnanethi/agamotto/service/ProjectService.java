package com.srikrishnanethi.agamotto.service;

import com.srikrishnanethi.agamotto.entities.Project;

import java.time.LocalDate;
import java.util.List;

public interface ProjectService {
    Project create(
            String ownerId,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate);

    Project getById(String projectId);

    List<Project> listAccessible(String userId);

    Project update(
            String projectId,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate);

    void delete(String projectId);
}
