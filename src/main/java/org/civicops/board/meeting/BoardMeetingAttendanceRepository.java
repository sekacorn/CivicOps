package org.civicops.board.meeting;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BoardMeetingAttendanceRepository
    extends JpaRepository<BoardMeetingAttendance, UUID>,
        JpaSpecificationExecutor<BoardMeetingAttendance> {
  Optional<BoardMeetingAttendance> findByMeetingIdAndBoardMemberId(UUID meeting, UUID member);

  boolean existsByMeetingIdAndBoardMemberId(UUID meeting, UUID member);

  List<BoardMeetingAttendance> findAllByOrganizationIdAndMeetingIdOrderByBoardMemberLastName(
      UUID org, UUID meeting);

  @Query(
      "select count(a) from BoardMeetingAttendance a where a.meeting.id=:meeting and a.attendanceStatus in ('PRESENT','REMOTE')")
  long quorumCount(@Param("meeting") UUID meeting);
}
