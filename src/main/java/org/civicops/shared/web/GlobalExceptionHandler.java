package org.civicops.shared.web;

import jakarta.validation.ConstraintViolationException;
import org.civicops.shared.exception.CivicOpsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(error -> error.getField(), error -> error.getDefaultMessage(), (left, right) -> left));
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "VALIDATION_ERROR", "Request validation failed", errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> status(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), status.name(), exception.getReason(), Map.of()));
    }

    @ExceptionHandler(CivicOpsException.class)
    ResponseEntity<ApiError> business(CivicOpsException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ApiError(Instant.now(),
                exception.getStatus().value(), exception.getCode(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraint(ConstraintViolationException exception) {
        Map<String, String> errors = exception.getConstraintViolations().stream().collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(), violation -> violation.getMessage(), (left, right) -> left));
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "VALIDATION_ERROR",
                "Request validation failed", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable() {
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "MALFORMED_REQUEST",
                "The request body is malformed or contains an unsupported value", Map.of()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(Instant.now(), 409,
                "DATA_CONFLICT", "The request conflicts with existing data", Map.of()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> accessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(Instant.now(), 403,
                "ACCESS_DENIED", "Access to this organization resource is denied", Map.of()));
    }
}
