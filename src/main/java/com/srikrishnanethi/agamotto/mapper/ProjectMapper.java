package com.srikrishnanethi.agamotto.mapper;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.dto.response.ProjectResponse;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

	public ProjectResponse toResponse(Project project) {
		return new ProjectResponse(
				project.getId(),
				project.getOwner().getId(),
				project.getName(),
				project.getDescription(),
				project.getStartDate(),
				project.getEndDate(),
				project.getEstimatedEffortHours(),
				project.getCreatedAt());
	}
}
