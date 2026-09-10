package org.civicops.board.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public final class BoardReportDtos {
  private BoardReportDtos() {}

  public record Summary(
      UUID organizationId,
      LocalDate from,
      LocalDate to,
      long activeBoardMembers,
      long activeCommittees,
      long meetingsInPeriod,
      BigDecimal averageAttendanceRate,
      long quorumFailures,
      long motionsPassed,
      long motionsFailed,
      long resolutionsAdopted) {}

  public record MemberAttendance(
      UUID boardMemberId,
      String memberName,
      long meetingsRecorded,
      long present,
      long remote,
      long absent,
      long excused,
      BigDecimal attendanceRate) {}

  public record Attendance(
      UUID organizationId,
      LocalDate from,
      LocalDate to,
      long totalMeetings,
      long quorumMet,
      long quorumNotMet,
      long remoteAttendances,
      List<MemberAttendance> members) {}

  public record Voting(
      UUID organizationId,
      LocalDate from,
      LocalDate to,
      long motionsPassed,
      long motionsFailed,
      long yesVotes,
      long noVotes,
      long abstainVotes,
      BigDecimal participationRate,
      long resolutionsAdopted) {}
}
