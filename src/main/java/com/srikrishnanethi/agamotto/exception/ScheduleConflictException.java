package com.srikrishnanethi.agamotto.exception;

/**
 * Raised when a SCHEDULED block would overlap another SCHEDULED block on the same plan (T10).
 */
public class ScheduleConflictException extends RuntimeException {

	public ScheduleConflictException(String message) {
		super(message);
	}
}
