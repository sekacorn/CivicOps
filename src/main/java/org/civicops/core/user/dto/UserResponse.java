package org.civicops.core.user.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.core.user.User;

public record UserResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    boolean active,
    boolean emailVerified,
    Instant createdAt,
    Instant updatedAt) {
  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.isActive(),
        user.isEmailVerified(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
