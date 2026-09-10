package org.civicops.board.member;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;

public final class BoardMemberDtos {
  private BoardMemberDtos() {}

  public record Create(
      @NotBlank @Size(max = 100) String firstName,
      @NotBlank @Size(max = 100) String lastName,
      @Email @Size(max = 320) String email,
      @Size(max = 50) String phone,
      @Size(max = 150) String title,
      @NotNull LocalDate joinedDate,
      @Size(max = 5000) String notes,
      UUID userId) {}

  public record Update(
      @Size(max = 100) String firstName,
      @Size(max = 100) String lastName,
      @Email @Size(max = 320) String email,
      @Size(max = 50) String phone,
      @Size(max = 150) String title,
      LocalDate joinedDate,
      @Size(max = 5000) String notes) {}

  public record Summary(
      UUID id,
      UUID userId,
      String firstName,
      String lastName,
      String title,
      boolean active,
      LocalDate joinedDate,
      Instant createdAt) {
    public static Summary from(BoardMember m) {
      return new Summary(
          m.getId(),
          m.getUser() == null ? null : m.getUser().getId(),
          m.getFirstName(),
          m.getLastName(),
          m.getTitle(),
          m.isActive(),
          m.getJoinedDate(),
          m.getCreatedAt());
    }
  }

  public record Detail(
      UUID id,
      UUID organizationId,
      UUID userId,
      String firstName,
      String lastName,
      String email,
      String phone,
      String title,
      boolean active,
      LocalDate joinedDate,
      String notes,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    public static Detail from(BoardMember m) {
      return new Detail(
          m.getId(),
          m.getOrganization().getId(),
          m.getUser() == null ? null : m.getUser().getId(),
          m.getFirstName(),
          m.getLastName(),
          m.getEmail(),
          m.getPhone(),
          m.getTitle(),
          m.isActive(),
          m.getJoinedDate(),
          m.getNotes(),
          m.getCreatedAt(),
          m.getUpdatedAt(),
          m.getVersion());
    }
  }

  public record CreateTerm(@NotNull LocalDate termStart, @NotNull LocalDate termEnd) {}

  public record EndTerm(@NotNull BoardTermStatus status) {}

  public record TermResponse(
      UUID id,
      UUID memberId,
      LocalDate termStart,
      LocalDate termEnd,
      BoardTermStatus status,
      Instant createdAt) {
    public static TermResponse from(BoardTerm t) {
      return new TermResponse(
          t.getId(),
          t.getBoardMember().getId(),
          t.getTermStart(),
          t.getTermEnd(),
          t.getStatus(),
          t.getCreatedAt());
    }
  }

  public record CreateOfficer(
      @NotNull BoardOfficerRole officerRole,
      @Size(max = 150) String otherTitle,
      @NotNull LocalDate startDate,
      LocalDate endDate) {}

  public record EndOfficer(@NotNull LocalDate endDate) {}

  public record OfficerResponse(
      UUID id,
      UUID memberId,
      BoardOfficerRole officerRole,
      String otherTitle,
      LocalDate startDate,
      LocalDate endDate,
      boolean active) {
    public static OfficerResponse from(BoardOfficerAssignment o) {
      return new OfficerResponse(
          o.getId(),
          o.getBoardMember().getId(),
          o.getOfficerRole(),
          o.getOtherTitle(),
          o.getStartDate(),
          o.getEndDate(),
          o.isActive());
    }
  }
}
