package com.srikrishnanethi.agamotto.exception;

import com.srikrishnanethi.agamotto.dto.response.ErrorResponse;
import com.srikrishnanethi.agamotto.entities.enums.ReportPeriod;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void dataTooLongIsBadRequestWithoutSqlDetails() {
		MockHttpServletRequest request = request("/api/projects/p1/schedule");
		SQLException sql = new SQLException(
				"Data truncation: Data too long for column 'reason' at row 1",
				"22001",
				1406);
		DataIntegrityViolationException ex =
				new DataIntegrityViolationException("could not execute statement", sql);

		ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(ex, request);

		assertEquals(400, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("A value is too long to save. Shorten the text and try again.",
				response.getBody().message());
		assertFalse(response.getBody().message().toLowerCase().contains("column"));
	}

	@Test
	void foreignKeyConflictIsConflictWithoutSqlDetails() {
		MockHttpServletRequest request = request("/api/projects/p1");
		SQLException sql = new SQLException(
				"Cannot delete or update a parent row: a foreign key constraint fails",
				"23000",
				1451);
		DataIntegrityViolationException ex =
				new DataIntegrityViolationException("could not execute statement", sql);

		ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(ex, request);

		assertEquals(409, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("This change conflicts with existing data.", response.getBody().message());
	}

	@Test
	void missingOptionalIsNotFound() {
		MockHttpServletRequest request = request("/api/users/missing");
		ResponseEntity<ErrorResponse> response =
				handler.handleNoSuchElement(new NoSuchElementException("No value present"), request);

		assertEquals(404, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("Resource not found", response.getBody().message());
	}

	@Test
	void unreadableJsonDoesNotLeakJacksonText() {
		MockHttpServletRequest request = request("/api/projects");
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
				"JSON parse error: Cannot deserialize value of type `java.time.LocalDate` from String \"foo\": "
						+ "Failed to deserialize java.time.LocalDate: (java.time.format.DateTimeParseException) "
						+ "Text 'foo' could not be parsed at index 0",
				new MockHttpInputMessage(new byte[0]));

		ResponseEntity<ErrorResponse> response = handler.handleUnreadableBody(ex, request);

		assertEquals(400, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("Invalid date or time format.", response.getBody().message());
		assertFalse(response.getBody().message().contains("JSON parse error"));
		assertFalse(response.getBody().message().contains("deserialize"));
		assertFalse(response.getBody().message().contains("DateTimeParseException"));
	}

	@Test
	void missingRequestBodyIsBadRequestWithoutJacksonText() {
		MockHttpServletRequest request = request("/api/projects/p1/schedules");
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
				"Required request body is missing: public com.srikrishnanethi.agamotto.dto.response"
						+ ".SchedulePlanResponse generate(java.lang.String, ...)",
				new MockHttpInputMessage(new byte[0]));

		ResponseEntity<ErrorResponse> response = handler.handleUnreadableBody(ex, request);

		assertEquals(400, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("Request body is required.", response.getBody().message());
		assertFalse(response.getBody().message().contains("SchedulePlanResponse"));
	}

	@Test
	void enumQueryParamMismatchDoesNotLeakConversionText() {
		MockHttpServletRequest request = request("/api/dashboard");
		MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
				"WEEKLYY",
				ReportPeriod.class,
				"period",
				null,
				new IllegalArgumentException(
						"Failed to convert value of type 'java.lang.String' to required type"));

		ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

		assertEquals(400, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().message().contains("period"));
		assertTrue(response.getBody().message().contains("WEEKLY"));
		assertFalse(response.getBody().message().contains("java.lang.String"));
		assertFalse(response.getBody().message().contains("Failed to convert"));
	}

	@Test
	void forbiddenIs403WithoutStack() {
		MockHttpServletRequest request = request("/api/users/other");
		ResponseEntity<ErrorResponse> response = handler.handleForbidden(
				new ForbiddenException("You cannot access another user's data"), request);
		assertEquals(403, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertEquals("You cannot access another user's data", response.getBody().message());
	}

	private static MockHttpServletRequest request(String uri) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI(uri);
		return request;
	}
}
