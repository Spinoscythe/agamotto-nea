package com.srikrishnanethi.agamotto.repositories;

import com.srikrishnanethi.agamotto.entities.ProjectMember;
import com.srikrishnanethi.agamotto.entities.enums.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, String> {

	Optional<ProjectMember> findByProjectIdAndUserId(String projectId, String userId);

	List<ProjectMember> findByProjectIdOrderByJoinedAtAsc(String projectId);

	List<ProjectMember> findByUserId(String userId);

	long countByProjectIdAndRole(String projectId, ProjectRole role);

	boolean existsByProjectIdAndUserId(String projectId, String userId);

	@Query(nativeQuery = true, value = "SELECT project_id FROM project_members where user_id = :userId")
	List<String> findProjectIdsByUserId(@Param("userId") String userId);
}
