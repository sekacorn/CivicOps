package org.civicops.board.minutes;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.board.meeting.BoardMeeting;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "board_meeting_minutes")
public class BoardMeetingMinutes extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "meeting_id")
  private BoardMeeting meeting;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String draftContent;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MinutesStatus status = MinutesStatus.DRAFT;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prepared_by_user_id")
  private User preparedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_by_user_id")
  private User approvedBy;

  private Instant submittedAt;
  private Instant approvedAt;

  protected BoardMeetingMinutes() {}

  public BoardMeetingMinutes(BoardMeeting m, String content, User preparer) {
    organization = m.getOrganization();
    meeting = m;
    preparedBy = preparer;
    update(content);
  }

  public void update(String content) {
    if (status != MinutesStatus.DRAFT)
      throw new BusinessRuleException("MINUTES_IMMUTABLE", "Only draft minutes may be edited");
    if (content == null || content.isBlank())
      throw new BusinessRuleException("MINUTES_CONTENT_REQUIRED", "Minutes content is required");
    draftContent = content.trim();
  }

  public void submit(Instant now) {
    if (status != MinutesStatus.DRAFT)
      throw new BusinessRuleException(
          "INVALID_MINUTES_TRANSITION", "Only draft minutes may be submitted");
    status = MinutesStatus.SUBMITTED;
    submittedAt = now;
  }

  public void approve(User approver, Instant now) {
    if (status != MinutesStatus.SUBMITTED)
      throw new BusinessRuleException(
          "INVALID_MINUTES_TRANSITION", "Only submitted minutes may be approved");
    status = MinutesStatus.APPROVED;
    approvedBy = approver;
    approvedAt = now;
  }

  public Organization getOrganization() {
    return organization;
  }

  public BoardMeeting getMeeting() {
    return meeting;
  }

  public String getDraftContent() {
    return draftContent;
  }

  public MinutesStatus getStatus() {
    return status;
  }

  public User getPreparedBy() {
    return preparedBy;
  }

  public User getApprovedBy() {
    return approvedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }
}
