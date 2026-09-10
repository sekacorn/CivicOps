package org.civicops.board.minutes;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class BoardMinutesDtos {
  private BoardMinutesDtos() {}

  public record Put(@NotBlank @Size(max = 100000) String content) {}

  public record Response(
      UUID id,
      UUID meetingId,
      String content,
      MinutesStatus status,
      UUID preparedByUserId,
      UUID approvedByUserId,
      Instant submittedAt,
      Instant approvedAt,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    public static Response from(BoardMeetingMinutes m) {
      return new Response(
          m.getId(),
          m.getMeeting().getId(),
          m.getDraftContent(),
          m.getStatus(),
          m.getPreparedBy().getId(),
          m.getApprovedBy() == null ? null : m.getApprovedBy().getId(),
          m.getSubmittedAt(),
          m.getApprovedAt(),
          m.getCreatedAt(),
          m.getUpdatedAt(),
          m.getVersion());
    }
  }
}
