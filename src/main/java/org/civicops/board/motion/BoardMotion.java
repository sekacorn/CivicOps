package org.civicops.board.motion;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.board.agenda.BoardAgendaItem;
import org.civicops.board.meeting.BoardMeeting;
import org.civicops.board.member.BoardMember;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "board_motion")
public class BoardMotion extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "meeting_id")
  private BoardMeeting meeting;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "agenda_item_id")
  private BoardAgendaItem agendaItem;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String motionText;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "moved_by_board_member_id")
  private BoardMember movedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "seconded_by_board_member_id")
  private BoardMember secondedBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MotionStatus status = MotionStatus.PROPOSED;

  @Column(nullable = false)
  private Instant openedAt;

  private Instant closedAt;

  protected BoardMotion() {}

  public BoardMotion(
      BoardMeeting meeting, BoardAgendaItem agenda, String text, BoardMember mover, Instant now) {
    organization = meeting.getOrganization();
    this.meeting = meeting;
    agendaItem = agenda;
    motionText = text.trim();
    movedBy = mover;
    openedAt = now;
    if (text.isBlank())
      throw new BusinessRuleException("INVALID_MOTION", "Motion text is required");
  }

  public void second(BoardMember member) {
    if (status != MotionStatus.PROPOSED)
      throw new BusinessRuleException(
          "INVALID_MOTION_TRANSITION", "Only proposed motions may be seconded");
    if (member.getId().equals(movedBy.getId()))
      throw new BusinessRuleException("MOTION_SELF_SECOND", "Mover cannot second the same motion");
    secondedBy = member;
    status = MotionStatus.SECONDED;
  }

  public void openVoting() {
    if (status != MotionStatus.SECONDED)
      throw new BusinessRuleException(
          "INVALID_MOTION_TRANSITION", "Only seconded motions may open voting");
    status = MotionStatus.VOTING;
  }

  public void close(boolean passed, Instant now) {
    if (status != MotionStatus.VOTING)
      throw new BusinessRuleException(
          "INVALID_MOTION_TRANSITION", "Only voting motions may be closed");
    status = passed ? MotionStatus.PASSED : MotionStatus.FAILED;
    closedAt = now;
  }

  public void withdraw(Instant now) {
    if (status != MotionStatus.PROPOSED)
      throw new BusinessRuleException(
          "INVALID_MOTION_TRANSITION", "Only proposed motions may be withdrawn");
    status = MotionStatus.WITHDRAWN;
    closedAt = now;
  }

  public void table(Instant now) {
    if (status != MotionStatus.SECONDED)
      throw new BusinessRuleException(
          "INVALID_MOTION_TRANSITION", "Only seconded motions may be tabled");
    status = MotionStatus.TABLED;
    closedAt = now;
  }

  public Organization getOrganization() {
    return organization;
  }

  public BoardMeeting getMeeting() {
    return meeting;
  }

  public BoardAgendaItem getAgendaItem() {
    return agendaItem;
  }

  public String getMotionText() {
    return motionText;
  }

  public BoardMember getMovedBy() {
    return movedBy;
  }

  public BoardMember getSecondedBy() {
    return secondedBy;
  }

  public MotionStatus getStatus() {
    return status;
  }

  public Instant getOpenedAt() {
    return openedAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }
}
