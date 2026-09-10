package org.civicops.board.motion;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class BoardMotionDtos {
  private BoardMotionDtos() {}

  public record Create(
      UUID agendaItemId,
      @NotBlank @Size(max = 10000) String motionText,
      @NotNull UUID movedByBoardMemberId) {}

  public record Second(@NotNull UUID secondedByBoardMemberId) {}

  public record VoteRequest(@NotNull VoteChoice choice) {}

  public record VoteResponse(
      UUID id, UUID motionId, UUID boardMemberId, VoteChoice choice, Instant castAt) {
    public static VoteResponse from(BoardVote v) {
      return new VoteResponse(
          v.getId(),
          v.getMotion().getId(),
          v.getBoardMember().getId(),
          v.getChoice(),
          v.getCastAt());
    }
  }

  public record Response(
      UUID id,
      UUID meetingId,
      UUID agendaItemId,
      String motionText,
      UUID movedByBoardMemberId,
      UUID secondedByBoardMemberId,
      MotionStatus status,
      Instant openedAt,
      Instant closedAt,
      long yesVotes,
      long noVotes,
      long abstainVotes,
      Instant createdAt,
      long version) {}
}
