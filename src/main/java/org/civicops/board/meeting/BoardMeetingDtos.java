package org.civicops.board.meeting;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class BoardMeetingDtos {
  private BoardMeetingDtos() {}

  public record Create(
      @NotBlank @Size(max = 200) String title,
      @NotNull BoardMeetingType meetingType,
      @NotNull Instant startDateTime,
      @NotNull Instant endDateTime,
      @Size(max = 300) String location,
      @Size(max = 500) String virtualMeetingUrl,
      @Min(1) int quorumRequired) {}

  public record Update(
      @Size(max = 200) String title,
      BoardMeetingType meetingType,
      Instant startDateTime,
      Instant endDateTime,
      @Size(max = 300) String location,
      @Size(max = 500) String virtualMeetingUrl,
      @Min(1) Integer quorumRequired) {}

  public record Response(
      UUID id,
      UUID organizationId,
      String title,
      BoardMeetingType meetingType,
      Instant startDateTime,
      Instant endDateTime,
      String location,
      String virtualMeetingUrl,
      BoardMeetingStatus status,
      int quorumRequired,
      UUID createdByUserId,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    public static Response from(BoardMeeting m) {
      return new Response(
          m.getId(),
          m.getOrganization().getId(),
          m.getTitle(),
          m.getMeetingType(),
          m.getStartDateTime(),
          m.getEndDateTime(),
          m.getLocation(),
          m.getVirtualMeetingUrl(),
          m.getStatus(),
          m.getQuorumRequired(),
          m.getCreatedBy().getId(),
          m.getCreatedAt(),
          m.getUpdatedAt(),
          m.getVersion());
    }
  }

  public record AttendanceRequest(
      @NotNull UUID boardMemberId,
      @NotNull AttendanceStatus attendanceStatus,
      @Size(max = 5000) String notes) {}

  public record AttendanceUpdate(
      @NotNull AttendanceStatus attendanceStatus, @Size(max = 5000) String notes) {}

  public record AttendanceResponse(
      UUID id,
      UUID meetingId,
      UUID boardMemberId,
      String memberName,
      AttendanceStatus attendanceStatus,
      Instant checkedInAt,
      String notes) {
    public static AttendanceResponse from(BoardMeetingAttendance a) {
      return new AttendanceResponse(
          a.getId(),
          a.getMeeting().getId(),
          a.getBoardMember().getId(),
          a.getBoardMember().getFirstName() + " " + a.getBoardMember().getLastName(),
          a.getAttendanceStatus(),
          a.getCheckedInAt(),
          a.getNotes());
    }
  }

  public record QuorumResponse(
      UUID meetingId,
      int quorumRequired,
      long presentOrRemote,
      boolean quorumMet,
      BigDecimal attendanceRate) {}
}
