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
 *
 * <p>No collection is used here on purpose: score is a scalar so the placer's
 * {@code PriorityQueue} comparator can call this in O(1) field work per comparison
 * (not a scan of other tasks).
 */
@Component
public class ScoringStrategy {

	/**
	 * Urgency for due-today / overdue tasks. Strictly above due-tomorrow ({@code 1.0})
	 * so {@code Math.max(1, daysUntil)} cannot collapse overdue into the same bucket.
	 */
	public static final double MAX_DEADLINE_URGENCY = 2.0;

	/**
	 * scoreTask: {@code (w_p * priority) + (w_u * deadlineUrgency) + (w_ed * estDuration)}.
	 *
	 * <p>Deadline urgency is {@code 1 / daysUntilDeadline} for future deadlines relative to
	 * {@code asOf}. Due today or overdue ({@code daysUntil <= 0}) uses
	 * {@link #MAX_DEADLINE_URGENCY} so nearer / missed deadlines outrank due-tomorrow.
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
		Objects.requireNonNull(task, "task");
		Objects.requireNonNull(asOf, "asOf");
		if (task.getDeadline() == null) {
			throw new IllegalArgumentException("task deadline is required");
		}
		long daysUntil = ChronoUnit.DAYS.between(asOf, task.getDeadline().toLocalDate());
		if (daysUntil <= 0) {
			return MAX_DEADLINE_URGENCY;
		}
		return 1.0 / daysUntil;
	}

	public double effectiveDurationHours(Task task) {
		Objects.requireNonNull(task, "task");
		Double corrected = task.getCorrectedDurationHours();
		double hours = corrected != null ? corrected : task.getEstimatedDurationHours();
		if (Double.isNaN(hours) || Double.isInfinite(hours)) {
			throw new IllegalArgumentException("duration must be a finite number");
		}
		if (hours < 0) {
			throw new IllegalArgumentException("duration must be >= 0");
		}
		return hours;
	}
}
