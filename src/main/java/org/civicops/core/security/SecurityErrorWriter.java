package org.civicops.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.civicops.shared.web.ApiError;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorWriter {
  private final ObjectMapper objectMapper;

  public SecurityErrorWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void unauthorized(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    write(request, response, 401, "UNAUTHORIZED", "Authentication is required or invalid");
  }

  public void forbidden(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    write(request, response, 403, "ACCESS_DENIED", "Access to this resource is denied");
  }

  private void write(
      HttpServletRequest request,
      HttpServletResponse response,
      int status,
      String code,
      String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(),
        new ApiError(Instant.now(), status, code, message, request.getRequestURI(), Map.of()));
  }
}
