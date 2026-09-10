package org.civicops.board.resolution;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;

public final class BoardResolutionDtos {
  private BoardResolutionDtos() {}

  public record Create(
      @NotBlank @Size(max = 100) String resolutionNumber,
      @NotBlank @Size(max = 200) String title,
      @NotBlank @Size(max = 100000) String text,
      @NotNull LocalDate adoptedDate) {}

  public record Response(
      UUID id,
      UUID meetingId,
      UUID motionId,
      String resolutionNumber,
      String title,
      String text,
      LocalDate adoptedDate,
      ResolutionStatus status,
      Instant rescindedAt,
      Instant createdAt) {
    public static Response from(BoardResolution r) {
      return new Response(
          r.getId(),
          r.getMeeting().getId(),
          r.getMotion() == null ? null : r.getMotion().getId(),
          r.getResolutionNumber(),
          r.getTitle(),
          r.getText(),
          r.getAdoptedDate(),
          r.getStatus(),
          r.getRescindedAt(),
          r.getCreatedAt());
    }
  }
}
