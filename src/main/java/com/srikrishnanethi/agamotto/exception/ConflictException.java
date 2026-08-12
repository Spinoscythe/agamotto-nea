package com.srikrishnanethi.agamotto.exception;

/**
 * Generic 409 conflict (non-email). Prefer {@link DuplicateEmailException} for register clashes.
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
