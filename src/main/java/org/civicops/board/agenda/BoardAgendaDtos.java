package org.civicops.board.agenda;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class BoardAgendaDtos {
  private BoardAgendaDtos() {}

  public record Create(
      @Min(1) int sequenceNumber,
      @NotBlank @Size(max = 200) String title,
      @Size(max = 5000) String description,
      @NotNull AgendaItemType itemType,
      @Size(max = 200) String presenter,
      @Min(1) Integer estimatedMinutes) {}

  public record Update(
      @Min(1) Integer sequenceNumber,
      @Size(max = 200) String title,
      @Size(max = 5000) String description,
      AgendaItemType itemType,
      @Size(max = 200) String presenter,
      @Min(1) Integer estimatedMinutes) {}

  public record Response(
      UUID id,
      UUID meetingId,
      int sequenceNumber,
      String title,
      String description,
      AgendaItemType itemType,
      String presenter,
      Integer estimatedMinutes,
      AgendaItemStatus status,
      Instant createdAt,
      long version) {
    public static Response from(BoardAgendaItem a) {
      return new Response(
          a.getId(),
          a.getMeeting().getId(),
          a.getSequenceNumber(),
          a.getTitle(),
          a.getDescription(),
          a.getItemType(),
          a.getPresenter(),
          a.getEstimatedMinutes(),
          a.getStatus(),
          a.getCreatedAt(),
          a.getVersion());
    }
  }
}
