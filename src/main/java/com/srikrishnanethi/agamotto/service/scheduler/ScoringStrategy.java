package com.srikrishnanethi.agamotto.service.scheduler;

import com.srikrishnanethi.agamotto.entities.Task;
import com.srikrishnanethi.agamotto.entities.UserProfile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Task scoring for Serenity / Crunch placement:
 * {@code w_p·priority + w_u·urgency + w_ed·estDuration}.
 */
@Component
public class ScoringStrategy {

	/**
	 * scoreTask: {@code (w_p * priority) + (w_u * deadlineUrgency) + (w_ed * estDuration)}.
	 *
	 * <p>Deadline urgency is {@code 1 / max(1, daysUntilDeadline)} relative to {@code asOf},
	 * so nearer deadlines contribute a larger score component.
	 */
	public double scoreTask(Task task, UserProfile profile, LocalDate asOf) {
		Objects.requireNonNull(task, "task");
		Objects.requireNonNull(profile, "profile");
		Objects.requireNonNull(asOf, "asOf");

		double urgency = deadlineUrgency(task, asOf);
		double duration = effectiveDurationHours(task);
		return (profile.getWeightPriority() * task.getPriority())
				+ (profile.getWeightUrgency() * urgency)
				+ (profile.getWeightDuration() * duration);
	}

	public double deadlineUrgency(Task task, LocalDate asOf) {
		long daysUntil = ChronoUnit.DAYS.between(asOf, task.getDeadline().toLocalDate());
		return 1.0 / Math.max(1L, daysUntil);
	}

	public double effectiveDurationHours(Task task) {
        return task.getCorrectedDurationHours() != null ? task.getCorrectedDurationHours() : task.getEstimatedDurationHours();
    }
}
