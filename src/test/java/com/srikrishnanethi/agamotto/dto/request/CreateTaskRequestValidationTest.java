package com.srikrishnanethi.agamotto.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateTaskRequestValidationTest {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void estimatedHoursAboveMaxAreRejected() {
		CreateTaskRequest request = new CreateTaskRequest(
				"user-1",
				"Essay",
				null,
				"work",
				3,
				LocalDateTime.now().plusDays(2),
				1000.1,
				2);

		assertFalse(validator.validate(request).isEmpty());
	}

	@Test
	void estimatedHoursAtMaxAreAccepted() {
		CreateTaskRequest request = new CreateTaskRequest(
				"user-1",
				"Essay",
				null,
				"work",
				3,
				LocalDateTime.now().plusDays(2),
				1000.0,
				2);

		assertTrue(validator.validate(request).isEmpty());
	}
}
