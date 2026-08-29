package org.civicops.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.civicops.shared.web.ApiError;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class SecurityErrorWriter {
    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void unauthorized(HttpServletResponse response) throws IOException {
        write(response, 401, "UNAUTHORIZED", "Authentication is required or invalid");
    }

    public void forbidden(HttpServletResponse response) throws IOException {
        write(response, 403, "ACCESS_DENIED", "Access to this resource is denied");
    }

    private void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                new ApiError(Instant.now(), status, code, message, Map.of()));
    }
}
