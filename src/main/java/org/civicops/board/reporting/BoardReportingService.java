package org.civicops.board.reporting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.civicops.board.committee.BoardCommitteeRepository;
import org.civicops.board.meeting.AttendanceStatus;
import org.civicops.board.meeting.BoardMeeting;
import org.civicops.board.meeting.BoardMeetingAttendance;
import org.civicops.board.meeting.BoardMeetingAttendanceRepository;
import org.civicops.board.meeting.BoardMeetingDtos;
import org.civicops.board.meeting.BoardMeetingRepository;
import org.civicops.board.meeting.BoardMeetingService;
import org.civicops.board.member.BoardMember;
import org.civicops.board.member.BoardMemberRepository;
import org.civicops.board.motion.BoardMotion;
import org.civicops.board.motion.BoardMotionRepository;
import org.civicops.board.motion.BoardVoteRepository;
import org.civicops.board.motion.MotionStatus;
import org.civicops.board.motion.VoteChoice;
import org.civicops.board.resolution.BoardResolution;
import org.civicops.board.resolution.BoardResolutionRepository;
import org.civicops.board.resolution.ResolutionStatus;
import org.civicops.shared.exception.BusinessRuleException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardReportingService {
  private final BoardMemberRepository members;
  private final BoardCommitteeRepository committees;
  private final BoardMeetingRepository meetings;
  private final BoardMeetingAttendanceRepository attendance;
  private final BoardMeetingService meetingService;
  private final BoardMotionRepository motions;
  private final BoardVoteRepository votes;
  private final BoardResolutionRepository resolutions;
  private final Clock clock;

  public BoardReportingService(
      BoardMemberRepository members,
      BoardCommitteeRepository committees,
      BoardMeetingRepository meetings,
      BoardMeetingAttendanceRepository attendance,
      BoardMeetingService meetingService,
      BoardMotionRepository motions,
      BoardVoteRepository votes,
      BoardResolutionRepository resolutions,
      Clock clock) {
    this.members = members;
    this.committees = committees;
    this.meetings = meetings;
    this.attendance = attendance;
    this.meetingService = meetingService;
    this.motions = motions;
    this.votes = votes;
    this.resolutions = resolutions;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public BoardReportDtos.Summary summary(UUID organizationId, LocalDate from, LocalDate to) {
    Range range = range(from, to);
    List<BoardMeeting> meetingRows = meetingRows(organizationId, range);
    List<BoardMeetingDtos.QuorumResponse> quorums =
        meetingRows.stream()
            .map(meeting -> meetingService.quorum(organizationId, meeting.getId()))
            .toList();
    List<BoardMotion> motionRows = motionRows(organizationId, range);
    List<BoardResolution> resolutionRows = resolutionRows(organizationId, range);
    BigDecimal averageAttendance =
        quorums.isEmpty()
            ? zero()
            : quorums.stream()
                .map(BoardMeetingDtos.QuorumResponse::attendanceRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(quorums.size()), 2, RoundingMode.HALF_UP);
    return new BoardReportDtos.Summary(
        organizationId,
        from,
        to,
        members.countByOrganizationIdAndActiveTrue(organizationId),
        committees.countByOrganizationIdAndActiveTrue(organizationId),
        meetingRows.size(),
        averageAttendance,
        quorums.stream().filter(quorum -> !quorum.quorumMet()).count(),
        count(motionRows, MotionStatus.PASSED),
        count(motionRows, MotionStatus.FAILED),
        resolutionRows.stream().filter(r -> r.getStatus() == ResolutionStatus.ADOPTED).count());
  }

  @Transactional(readOnly = true)
  public BoardReportDtos.Attendance attendance(UUID organizationId, LocalDate from, LocalDate to) {
    Range range = range(from, to);
    List<BoardMeeting> meetingRows = meetingRows(organizationId, range);
    Set<UUID> meetingIds =
        meetingRows.stream().map(BoardMeeting::getId).collect(Collectors.toSet());
    Specification<BoardMeetingAttendance> specification =
        (root, query, builder) ->
            builder.and(
                builder.equal(root.get("organization").get("id"), organizationId),
                root.get("meeting").get("id").in(meetingIds));
    List<BoardMeetingAttendance> rows =
        meetingIds.isEmpty() ? List.of() : attendance.findAll(specification);
    Map<BoardMember, List<BoardMeetingAttendance>> grouped =
        rows.stream().collect(Collectors.groupingBy(BoardMeetingAttendance::getBoardMember));
    List<BoardReportDtos.MemberAttendance> details =
        grouped.entrySet().stream()
            .map(entry -> memberAttendance(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(BoardReportDtos.MemberAttendance::memberName))
            .toList();
    long quorumMet =
        meetingRows.stream()
            .filter(meeting -> meetingService.quorum(organizationId, meeting.getId()).quorumMet())
            .count();
    return new BoardReportDtos.Attendance(
        organizationId,
        from,
        to,
        meetingRows.size(),
        quorumMet,
        meetingRows.size() - quorumMet,
        rows.stream().filter(row -> row.getAttendanceStatus() == AttendanceStatus.REMOTE).count(),
        details);
  }

  @Transactional(readOnly = true)
  public BoardReportDtos.Voting voting(UUID organizationId, LocalDate from, LocalDate to) {
    Range range = range(from, to);
    List<BoardMotion> motionRows = motionRows(organizationId, range);
    long yes = 0;
    long no = 0;
    long abstain = 0;
    long total = 0;
    for (BoardMotion motion : motionRows) {
      UUID motionId = motion.getId();
      yes += votes.countByMotionIdAndChoice(motionId, VoteChoice.YES);
      no += votes.countByMotionIdAndChoice(motionId, VoteChoice.NO);
      abstain += votes.countByMotionIdAndChoice(motionId, VoteChoice.ABSTAIN);
      total += votes.countByMotionId(motionId);
    }
    long votingOpportunities =
        members.countByOrganizationIdAndActiveTrue(organizationId) * motionRows.size();
    BigDecimal participationRate =
        votingOpportunities == 0
            ? zero()
            : BigDecimal.valueOf(total * 100)
                .divide(BigDecimal.valueOf(votingOpportunities), 2, RoundingMode.HALF_UP);
    return new BoardReportDtos.Voting(
        organizationId,
        from,
        to,
        count(motionRows, MotionStatus.PASSED),
        count(motionRows, MotionStatus.FAILED),
        yes,
        no,
        abstain,
        participationRate,
        resolutionRows(organizationId, range).stream()
            .filter(resolution -> resolution.getStatus() == ResolutionStatus.ADOPTED)
            .count());
  }

  private BoardReportDtos.MemberAttendance memberAttendance(
      BoardMember member, List<BoardMeetingAttendance> rows) {
    long present = count(rows, AttendanceStatus.PRESENT);
    long remote = count(rows, AttendanceStatus.REMOTE);
    return new BoardReportDtos.MemberAttendance(
        member.getId(),
        member.getFirstName() + " " + member.getLastName(),
        rows.size(),
        present,
        remote,
        count(rows, AttendanceStatus.ABSENT),
        count(rows, AttendanceStatus.EXCUSED),
        BigDecimal.valueOf((present + remote) * 100)
            .divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP));
  }

  private List<BoardMeeting> meetingRows(UUID organizationId, Range range) {
    Specification<BoardMeeting> specification =
        (root, query, builder) ->
            builder.and(
                builder.equal(root.get("organization").get("id"), organizationId),
                builder.greaterThanOrEqualTo(root.get("startDateTime"), range.start()),
                builder.lessThan(root.get("startDateTime"), range.end()));
    return meetings.findAll(specification);
  }

  private List<BoardMotion> motionRows(UUID organizationId, Range range) {
    Specification<BoardMotion> specification =
        (root, query, builder) ->
            builder.and(
                builder.equal(root.get("organization").get("id"), organizationId),
                builder.greaterThanOrEqualTo(
                    root.get("meeting").get("startDateTime"), range.start()),
                builder.lessThan(root.get("meeting").get("startDateTime"), range.end()));
    return motions.findAll(specification);
  }

  private List<BoardResolution> resolutionRows(UUID organizationId, Range range) {
    LocalDate first = LocalDate.ofInstant(range.start(), ZoneOffset.UTC);
    LocalDate last = LocalDate.ofInstant(range.end().minusNanos(1), ZoneOffset.UTC);
    Specification<BoardResolution> specification =
        (root, query, builder) ->
            builder.and(
                builder.equal(root.get("organization").get("id"), organizationId),
                builder.greaterThanOrEqualTo(root.get("adoptedDate"), first),
                builder.lessThanOrEqualTo(root.get("adoptedDate"), last));
    return resolutions.findAll(specification);
  }

  private Range range(LocalDate from, LocalDate to) {
    if (from != null && to != null && to.isBefore(from)) {
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Board report end precedes start");
    }
    Instant start = from == null ? Instant.EPOCH : from.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end =
        to == null
            ? Instant.now(clock).plusSeconds(1)
            : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    return new Range(start, end);
  }

  private static long count(List<BoardMotion> motions, MotionStatus status) {
    return motions.stream().filter(motion -> motion.getStatus() == status).count();
  }

  private static long count(List<BoardMeetingAttendance> rows, AttendanceStatus status) {
    return rows.stream().filter(row -> row.getAttendanceStatus() == status).count();
  }

  private static BigDecimal zero() {
    return BigDecimal.ZERO.setScale(2);
  }

  private record Range(Instant start, Instant end) {}
}
