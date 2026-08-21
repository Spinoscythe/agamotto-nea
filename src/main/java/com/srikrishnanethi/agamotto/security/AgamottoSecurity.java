package com.srikrishnanethi.agamotto.security;

import com.srikrishnanethi.agamotto.entities.Project;
import com.srikrishnanethi.agamotto.entities.ScheduleBlock;
import com.srikrishnanethi.agamotto.entities.SchedulePlan;
import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.User;
import com.srikrishnanethi.agamotto.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AgamottoSecurity {
	private AgamottoSecurity() {
	}

	public static String currentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getPrincipal() == null || !auth.isAuthenticated()) {
			throw new ForbiddenException("Authentication required");
		}
		Object principal = auth.getPrincipal();
		if (!(principal instanceof String userId) || userId.isBlank()) {
			throw new ForbiddenException("Authentication required");
		}
		return userId;
	}

	public static void requireSelf(String userId) {
		if (userId != null && !userId.isBlank() && !currentUserId().equals(userId)) {
			throw new ForbiddenException("You cannot access another user's data");
		}
	}

	public static void requireOwner(Project project) {
		User owner = project == null ? null : project.getOwner();
		if (owner == null || owner.getId() == null || !currentUserId().equals(owner.getId())) {
			throw new ForbiddenException("You cannot access this project");
		}
	}

	public static void requireOwner(Task task) {
		if (task == null || task.getProject() == null) {
			throw new ForbiddenException("You cannot access this task");
		}
		requireOwner(task.getProject());
	}

	public static void requireOwner(SchedulePlan plan) {
		if (plan == null || plan.getProject() == null) {
			throw new ForbiddenException("You cannot access this schedule");
		}
		requireOwner(plan.getProject());
	}

	public static void requireOwner(ScheduleBlock block) {
		if (block == null || block.getSchedule() == null) {
			throw new ForbiddenException("You cannot access this schedule block");
		}
		requireOwner(block.getSchedule());
	}
}
