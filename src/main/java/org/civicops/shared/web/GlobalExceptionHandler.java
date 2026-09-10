package org.civicops.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.civicops.shared.exception.CivicOpsException;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler implements ResponseBodyAdvice<Object> {
  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return Page.class.isAssignableFrom(returnType.getParameterType());
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      org.springframework.http.server.ServerHttpRequest request,
      org.springframework.http.server.ServerHttpResponse response) {
    return body instanceof Page<?> page ? PageResponse.from(page) : body;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    Map<String, String> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    error -> error.getField(),
                    error -> error.getDefaultMessage(),
                    (left, right) -> left));
    return ResponseEntity.badRequest()
        .body(error(400, "VALIDATION_ERROR", "Request validation failed", request, errors));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ApiError> status(ResponseStatusException exception, HttpServletRequest request) {
    HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
    return ResponseEntity.status(status)
        .body(error(status.value(), status.name(), exception.getReason(), request, Map.of()));
  }

  @ExceptionHandler(CivicOpsException.class)
  ResponseEntity<ApiError> business(CivicOpsException exception, HttpServletRequest request) {
    return ResponseEntity.status(exception.getStatus())
        .body(
            error(
                exception.getStatus().value(),
                exception.getCode(),
                exception.getMessage(),
                request,
                Map.of()));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiError> constraint(
      ConstraintViolationException exception, HttpServletRequest request) {
    Map<String, String> errors =
        exception.getConstraintViolations().stream()
            .collect(
                Collectors.toMap(
                    violation -> violation.getPropertyPath().toString(),
                    violation -> violation.getMessage(),
                    (left, right) -> left));
    return ResponseEntity.badRequest()
        .body(error(400, "VALIDATION_ERROR", "Request validation failed", request, errors));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiError> unreadable(HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(
            error(
                400,
                "MALFORMED_REQUEST",
                "The request body is malformed or contains an unsupported value",
                request,
                Map.of()));
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  ResponseEntity<ApiError> invalidParameter(Exception exception, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(
            error(
                400,
                "INVALID_REQUEST_PARAMETER",
                "A request parameter is missing or has an unsupported value",
                request,
                Map.of()));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiError> noResource(
      NoResourceFoundException exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            error(
                404,
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found",
                request,
                Map.of()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiError> integrity(HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            error(
                409,
                "DATA_CONFLICT",
                "The request conflicts with existing data",
                request,
                Map.of()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiError> accessDenied(HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            error(
                403,
                "ACCESS_DENIED",
                "Access to this organization resource is denied",
                request,
                Map.of()));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(error(500, "INTERNAL_ERROR", "An unexpected error occurred", request, Map.of()));
  }

  private ApiError error(
      int status,
      String code,
      String message,
      HttpServletRequest request,
      Map<String, String> fields) {
    return new ApiError(Instant.now(), status, code, message, request.getRequestURI(), fields);
  }
}
