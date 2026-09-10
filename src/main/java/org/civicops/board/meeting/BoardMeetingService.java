package org.civicops.board.meeting;

import java.math.*;
import java.time.*;
import java.util.*;
import org.civicops.board.member.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.UserService;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardMeetingService {
  private final BoardMeetingRepository meetings;
  private final BoardMeetingAttendanceRepository attendance;
  private final BoardMemberService members;
  private final BoardTermRepository terms;
  private final OrganizationService organizations;
  private final UserService users;
  private final Clock clock;

  public BoardMeetingService(
      BoardMeetingRepository m,
      BoardMeetingAttendanceRepository a,
      BoardMemberService bm,
      BoardTermRepository t,
      OrganizationService o,
      UserService u,
      Clock c) {
    meetings = m;
    attendance = a;
    members = bm;
    terms = t;
    organizations = o;
    users = u;
    clock = c;
  }

  @Transactional
  public BoardMeetingDtos.Response create(UUID org, UUID actor, BoardMeetingDtos.Create r) {
    return BoardMeetingDtos.Response.from(
        meetings.save(
            new BoardMeeting(
                organizations.requireEntity(org),
                users.requireEntity(actor),
                r.title(),
                r.meetingType(),
                r.startDateTime(),
                r.endDateTime(),
                r.location(),
                r.virtualMeetingUrl(),
                r.quorumRequired())));
  }

  @Transactional
  public BoardMeetingDtos.Response update(UUID org, UUID id, BoardMeetingDtos.Update r) {
    BoardMeeting m = require(org, id);
    m.update(
        r.title(),
        r.meetingType(),
        r.startDateTime(),
        r.endDateTime(),
        r.location(),
        r.virtualMeetingUrl(),
        r.quorumRequired());
    return BoardMeetingDtos.Response.from(m);
  }

  @Transactional
  public BoardMeetingDtos.Response transition(UUID org, UUID id, BoardMeetingStatus target) {
    BoardMeeting m =
        meetings
            .findLocked(org, id)
            .orElseThrow(() -> new ResourceNotFoundException("Board meeting", id));
    m.transition(target);
    return BoardMeetingDtos.Response.from(m);
  }

  @Transactional(readOnly = true)
  public BoardMeeting require(UUID org, UUID id) {
    return meetings
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Board meeting", id));
  }

  @Transactional(readOnly = true)
  public BoardMeetingDtos.Response detail(UUID org, UUID id) {
    return BoardMeetingDtos.Response.from(require(org, id));
  }

  @Transactional(readOnly = true)
  public Page<BoardMeetingDtos.Response> list(
      UUID org,
      BoardMeetingStatus status,
      BoardMeetingType type,
      Instant from,
      Instant to,
      Pageable p) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Meeting filter end precedes start");
    Specification<BoardMeeting> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (type != null) s = s.and((r, q, c) -> c.equal(r.get("meetingType"), type));
    if (from != null) s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("startDateTime"), from));
    if (to != null) s = s.and((r, q, c) -> c.lessThan(r.get("startDateTime"), to));
    return meetings.findAll(s, p).map(BoardMeetingDtos.Response::from);
  }

  @Transactional
  public BoardMeetingDtos.AttendanceResponse recordAttendance(
      UUID org, UUID meetingId, BoardMeetingDtos.AttendanceRequest r) {
    BoardMeeting meeting = requireOpenForAttendance(org, meetingId);
    BoardMember member = members.require(org, r.boardMemberId());
    members.requireEligible(org, member, date(meeting));
    if (attendance.existsByMeetingIdAndBoardMemberId(meetingId, member.getId()))
      throw new ConflictException(
          "DUPLICATE_MEETING_ATTENDANCE", "Attendance already exists for this member");
    return BoardMeetingDtos.AttendanceResponse.from(
        attendance.save(
            new BoardMeetingAttendance(
                meeting, member, r.attendanceStatus(), Instant.now(clock), r.notes())));
  }

  @Transactional
  public BoardMeetingDtos.AttendanceResponse selfAttendance(
      UUID org, UUID meetingId, UUID user, BoardMeetingDtos.AttendanceUpdate r) {
    BoardMeeting meeting = requireOpenForAttendance(org, meetingId);
    BoardMember member = members.linked(org, user);
    members.requireEligible(org, member, date(meeting));
    Optional<BoardMeetingAttendance> existing =
        attendance.findByMeetingIdAndBoardMemberId(meetingId, member.getId());
    if (existing.isPresent()) {
      existing.get().update(r.attendanceStatus(), Instant.now(clock), r.notes());
      return BoardMeetingDtos.AttendanceResponse.from(existing.get());
    }
    return BoardMeetingDtos.AttendanceResponse.from(
        attendance.save(
            new BoardMeetingAttendance(
                meeting, member, r.attendanceStatus(), Instant.now(clock), r.notes())));
  }

  @Transactional
  public BoardMeetingDtos.AttendanceResponse updateAttendance(
      UUID org, UUID meetingId, UUID memberId, BoardMeetingDtos.AttendanceUpdate r) {
    BoardMeeting m = require(org, meetingId);
    if (m.getStatus() == BoardMeetingStatus.COMPLETED
        || m.getStatus() == BoardMeetingStatus.CANCELLED)
      throw new BusinessRuleException(
          "ATTENDANCE_FINALIZED", "Terminal meeting attendance cannot be changed");
    BoardMeetingAttendance a =
        attendance
            .findByMeetingIdAndBoardMemberId(meetingId, memberId)
            .orElseThrow(() -> new ResourceNotFoundException("Board meeting attendance", memberId));
    if (!a.getOrganization().getId().equals(org))
      throw new ResourceNotFoundException("Board meeting attendance", memberId);
    a.update(r.attendanceStatus(), Instant.now(clock), r.notes());
    return BoardMeetingDtos.AttendanceResponse.from(a);
  }

  @Transactional(readOnly = true)
  public List<BoardMeetingDtos.AttendanceResponse> attendance(UUID org, UUID meeting) {
    require(org, meeting);
    return attendance
        .findAllByOrganizationIdAndMeetingIdOrderByBoardMemberLastName(org, meeting)
        .stream()
        .map(BoardMeetingDtos.AttendanceResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public BoardMeetingDtos.QuorumResponse quorum(UUID org, UUID meetingId) {
    BoardMeeting m = require(org, meetingId);
    long present = attendance.quorumCount(meetingId), eligible = terms.eligibleCount(org, date(m));
    BigDecimal rate =
        eligible == 0
            ? BigDecimal.ZERO.setScale(2)
            : BigDecimal.valueOf(present * 100)
                .divide(BigDecimal.valueOf(eligible), 2, RoundingMode.HALF_UP);
    return new BoardMeetingDtos.QuorumResponse(
        meetingId, m.getQuorumRequired(), present, present >= m.getQuorumRequired(), rate);
  }

  private BoardMeeting requireOpenForAttendance(UUID org, UUID id) {
    BoardMeeting meeting = require(org, id);
    if (meeting.getStatus() != BoardMeetingStatus.PUBLISHED
        && meeting.getStatus() != BoardMeetingStatus.IN_PROGRESS)
      throw new BusinessRuleException(
          "ATTENDANCE_NOT_OPEN", "Attendance requires a published or in-progress meeting");
    return meeting;
  }

  private static LocalDate date(BoardMeeting m) {
    return LocalDate.ofInstant(m.getStartDateTime(), ZoneOffset.UTC);
  }
}
