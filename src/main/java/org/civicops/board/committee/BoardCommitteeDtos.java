package org.civicops.board.committee;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;

public final class BoardCommitteeDtos {
  private BoardCommitteeDtos() {}

  public record Create(
      @NotBlank @Size(max = 200) String name, @Size(max = 5000) String description) {}

  public record Update(@Size(max = 200) String name, @Size(max = 5000) String description) {}

  public record Response(
      UUID id, String name, String description, boolean active, Instant createdAt) {
    public static Response from(BoardCommittee c) {
      return new Response(
          c.getId(), c.getName(), c.getDescription(), c.isActive(), c.getCreatedAt());
    }
  }

  public record AddMember(
      @NotNull UUID boardMemberId, @Size(max = 100) String role, @NotNull LocalDate startDate) {}

  public record EndMember(@NotNull LocalDate endDate) {}

  public record MembershipResponse(
      UUID id,
      UUID committeeId,
      UUID boardMemberId,
      String boardMemberName,
      String role,
      LocalDate startDate,
      LocalDate endDate,
      boolean active) {
    public static MembershipResponse from(BoardCommitteeMembership m) {
      return new MembershipResponse(
          m.getId(),
          m.getCommittee().getId(),
          m.getBoardMember().getId(),
          m.getBoardMember().getFirstName() + " " + m.getBoardMember().getLastName(),
          m.getCommitteeRole(),
          m.getStartDate(),
          m.getEndDate(),
          m.isActive());
    }
  }
}
