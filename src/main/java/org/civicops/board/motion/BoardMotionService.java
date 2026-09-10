package org.civicops.board.motion;

import java.time.*;
import java.util.*;
import org.civicops.board.agenda.*;
import org.civicops.board.meeting.*;
import org.civicops.board.member.*;
import org.civicops.core.user.UserService;
import org.civicops.shared.exception.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardMotionService {
  private final BoardMotionRepository motions;
  private final BoardVoteRepository votes;
  private final BoardMeetingService meetings;
  private final BoardAgendaService agenda;
  private final BoardMemberService members;
  private final UserService users;
  private final Clock clock;

  public BoardMotionService(
      BoardMotionRepository m,
      BoardVoteRepository v,
      BoardMeetingService meetings,
      BoardAgendaService a,
      BoardMemberService bm,
      UserService u,
      Clock c) {
    motions = m;
    votes = v;
    this.meetings = meetings;
    agenda = a;
    members = bm;
    users = u;
    clock = c;
  }

  @Transactional
  public BoardMotionDtos.Response create(UUID org, UUID meetingId, BoardMotionDtos.Create r) {
    BoardMeeting meeting = meetings.require(org, meetingId);
    if (meeting.getStatus() != BoardMeetingStatus.PUBLISHED
        && meeting.getStatus() != BoardMeetingStatus.IN_PROGRESS)
      throw new BusinessRuleException(
          "MEETING_NOT_PUBLISHED", "Motions require a published or in-progress meeting");
    BoardMember mover = members.require(org, r.movedByBoardMemberId());
    members.requireEligible(org, mover, date(meeting));
    BoardAgendaItem item = r.agendaItemId() == null ? null : agenda.require(org, r.agendaItemId());
    if (item != null && !item.getMeeting().getId().equals(meetingId))
      throw new BusinessRuleException(
          "MOTION_AGENDA_MISMATCH", "Agenda item must belong to the motion meeting");
    return response(
        motions.save(new BoardMotion(meeting, item, r.motionText(), mover, Instant.now(clock))));
  }

  @Transactional
  public BoardMotionDtos.Response second(UUID org, UUID id, UUID memberId) {
    BoardMotion m = requireLocked(org, id);
    BoardMember member = members.require(org, memberId);
    members.requireEligible(org, member, date(m.getMeeting()));
    m.second(member);
    return response(m);
  }

  @Transactional
  public BoardMotionDtos.Response open(UUID org, UUID id) {
    BoardMotion m = requireLocked(org, id);
    if (m.getMeeting().getStatus() != BoardMeetingStatus.IN_PROGRESS)
      throw new BusinessRuleException(
          "MEETING_NOT_IN_PROGRESS", "Voting requires an in-progress meeting");
    m.openVoting();
    return response(m);
  }

  @Transactional
  public BoardMotionDtos.Response close(UUID org, UUID id) {
    BoardMotion m = requireLocked(org, id);
    BoardMeetingDtos.QuorumResponse quorum = meetings.quorum(org, m.getMeeting().getId());
    if (!quorum.quorumMet())
      throw new BusinessRuleException(
          "QUORUM_NOT_MET", "Motion cannot close without meeting quorum");
    long yes = votes.countByMotionIdAndChoice(id, VoteChoice.YES),
        no = votes.countByMotionIdAndChoice(id, VoteChoice.NO);
    m.close(yes > no, Instant.now(clock));
    return response(m);
  }

  @Transactional
  public BoardMotionDtos.Response withdraw(UUID org, UUID id) {
    BoardMotion m = requireLocked(org, id);
    m.withdraw(Instant.now(clock));
    return response(m);
  }

  @Transactional
  public BoardMotionDtos.Response table(UUID org, UUID id) {
    BoardMotion m = requireLocked(org, id);
    m.table(Instant.now(clock));
    return response(m);
  }

  @Transactional
  public BoardMotionDtos.VoteResponse voteSelf(UUID org, UUID id, UUID user, VoteChoice choice) {
    BoardMotion m = requireLocked(org, id);
    if (m.getStatus() != MotionStatus.VOTING)
      throw new BusinessRuleException(
          "MOTION_NOT_VOTING", "Votes may only be cast while motion is VOTING");
    BoardMember member = members.linked(org, user);
    members.requireEligible(org, member, date(m.getMeeting()));
    if (votes.existsByMotionIdAndBoardMemberId(id, member.getId()))
      throw new ConflictException(
          "DUPLICATE_BOARD_VOTE", "Board member already voted on this motion");
    try {
      return BoardMotionDtos.VoteResponse.from(
          votes.saveAndFlush(
              new BoardVote(m, member, choice, Instant.now(clock), users.requireEntity(user))));
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException(
          "DUPLICATE_BOARD_VOTE", "Board member already voted on this motion");
    }
  }

  @Transactional(readOnly = true)
  public BoardMotion require(UUID org, UUID id) {
    return motions
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Board motion", id));
  }

  private BoardMotion requireLocked(UUID org, UUID id) {
    return motions
        .findLocked(org, id)
        .orElseThrow(() -> new ResourceNotFoundException("Board motion", id));
  }

  @Transactional(readOnly = true)
  public BoardMotionDtos.Response detail(UUID org, UUID id) {
    return response(require(org, id));
  }

  @Transactional(readOnly = true)
  public Page<BoardMotionDtos.Response> list(
      UUID org, UUID meeting, MotionStatus status, Pageable p) {
    Specification<BoardMotion> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (meeting != null) s = s.and((r, q, c) -> c.equal(r.get("meeting").get("id"), meeting));
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    return motions.findAll(s, p).map(this::response);
  }

  @Transactional(readOnly = true)
  public List<BoardMotionDtos.VoteResponse> votes(UUID org, UUID motion) {
    require(org, motion);
    return votes.findAllByOrganizationIdAndMotionIdOrderByCastAt(org, motion).stream()
        .map(BoardMotionDtos.VoteResponse::from)
        .toList();
  }

  private BoardMotionDtos.Response response(BoardMotion m) {
    return new BoardMotionDtos.Response(
        m.getId(),
        m.getMeeting().getId(),
        m.getAgendaItem() == null ? null : m.getAgendaItem().getId(),
        m.getMotionText(),
        m.getMovedBy().getId(),
        m.getSecondedBy() == null ? null : m.getSecondedBy().getId(),
        m.getStatus(),
        m.getOpenedAt(),
        m.getClosedAt(),
        votes.countByMotionIdAndChoice(m.getId(), VoteChoice.YES),
        votes.countByMotionIdAndChoice(m.getId(), VoteChoice.NO),
        votes.countByMotionIdAndChoice(m.getId(), VoteChoice.ABSTAIN),
        m.getCreatedAt(),
        m.getVersion());
  }

  private static LocalDate date(BoardMeeting m) {
    return LocalDate.ofInstant(m.getStartDateTime(), ZoneOffset.UTC);
  }
}
