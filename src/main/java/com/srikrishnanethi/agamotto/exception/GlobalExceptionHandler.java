package com.srikrishnanethi.agamotto.exception;

import com.srikrishnanethi.agamotto.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(
			ResourceNotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler({
			DuplicateEmailException.class,
			ConflictException.class,
			ScheduleConflictException.class
	})
	public ResponseEntity<ErrorResponse> handleConflict(
			RuntimeException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorized(
			InvalidCredentialsException ex, HttpServletRequest request) {
		return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(
			IllegalArgumentException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableBody(
			HttpMessageNotReadableException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, unreadableBodyMessage(ex), request);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingParameter(
			MissingServletRequestParameterException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST,
				"Missing required parameter: " + ex.getParameterName(),
				request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(
			MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, typeMismatchMessage(ex), request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(
			ConstraintViolationException ex, HttpServletRequest request) {
		String message = ex.getConstraintViolations().stream()
				.map(v -> v.getPropertyPath() + ": " + v.getMessage())
				.collect(Collectors.joining("; "));
		if (message.isBlank()) {
			message = "Validation failed";
		}
		return build(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(
			MethodArgumentNotValidException ex, HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(this::formatFieldError)
				.collect(Collectors.joining("; "));
		if (message.isBlank()) {
			message = "Validation failed";
		}
		return build(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotSupported(
			HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
		return build(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported", request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
			HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
		return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type is not supported", request);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErrorResponse> handleResponseStatus(
			ResponseStatusException ex, HttpServletRequest request) {
		HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
		if (status == null) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		String message = ex.getReason() != null && !ex.getReason().isBlank()
				? ex.getReason()
				: status.getReasonPhrase();
		return build(status, message, request);
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ErrorResponse> handleNoSuchElement(
			NoSuchElementException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, "Resource not found", request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrity(
			DataIntegrityViolationException ex, HttpServletRequest request) {
		String root = rootMessage(ex);
		if (root != null && root.toLowerCase().contains("data too long")) {
			return build(HttpStatus.BAD_REQUEST,
					"A value is too long to save. Shorten the text and try again.",
					request);
		}
		return build(HttpStatus.CONFLICT,
				"This change conflicts with existing data.",
				request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFoundResource(
			NoResourceFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, "Resource not found", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(
			Exception ex, HttpServletRequest request) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request);
	}

	private String formatFieldError(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}

	static String unreadableBodyMessage(HttpMessageNotReadableException ex) {
		String raw = combinedMessages(ex).toLowerCase();
		if (raw.contains("required request body is missing")) {
			return "Request body is required.";
		}
		if (raw.contains("localdate") || raw.contains("localtime") || raw.contains("instant")
				|| raw.contains("datetimeparse")) {
			return "Invalid date or time format.";
		}
		if (raw.contains("not one of the values accepted for enum")) {
			return "Invalid value.";
		}
		if (raw.contains("cannot map `null`")
				|| raw.contains("cannot deserialize value of type `int`")
				|| raw.contains("cannot deserialize value of type `double`")
				|| raw.contains("cannot deserialize value of type `java.lang.integer`")
				|| raw.contains("cannot deserialize value of type `java.lang.double`")) {
			return "A number was missing or invalid.";
		}
		return "Request body is invalid or unreadable.";
	}

	static String typeMismatchMessage(MethodArgumentTypeMismatchException ex) {
		String name = ex.getName() == null ? "parameter" : ex.getName();
		Class<?> type = ex.getRequiredType();
		if (type != null && type.isEnum()) {
			String allowed = Arrays.stream(type.getEnumConstants())
					.map(Object::toString)
					.collect(Collectors.joining(", "));
			return "Invalid value for " + name + ". Allowed values: " + allowed;
		}
		return "Invalid value for parameter '" + name + "'.";
	}

	private static String combinedMessages(Throwable ex) {
		StringBuilder sb = new StringBuilder();
		Throwable current = ex;
		int depth = 0;
		while (current != null && depth < 8) {
			if (current.getMessage() != null) {
				sb.append(current.getMessage()).append('\n');
			}
			current = current.getCause();
			depth++;
		}
		return sb.toString();
	}

	private static String rootMessage(Throwable ex) {
		Throwable current = ex;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return current.getMessage();
	}

	private ResponseEntity<ErrorResponse> build(
			HttpStatus status, String message, HttpServletRequest request) {
		ErrorResponse body = new ErrorResponse(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI());
		return ResponseEntity.status(status).body(body);
	}
}
