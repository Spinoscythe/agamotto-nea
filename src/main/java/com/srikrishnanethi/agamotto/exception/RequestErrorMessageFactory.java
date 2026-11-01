package com.srikrishnanethi.agamotto.exception;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Turns framework/Jackson exception text into safe, user-facing messages.
 */
final class RequestErrorMessageFactory {

    private RequestErrorMessageFactory() {
    }

    static String formatFieldError(FieldError error) {
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

    static String combinedMessages(Throwable ex) {
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

    static String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
