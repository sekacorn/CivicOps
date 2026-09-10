package org.civicops.board.meeting;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "board_meeting")
public class BoardMeeting extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @Column(nullable = false)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BoardMeetingType meetingType;

  @Column(nullable = false)
  private Instant startDateTime;

  @Column(nullable = false)
  private Instant endDateTime;

  private String location;
  private String virtualMeetingUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BoardMeetingStatus status = BoardMeetingStatus.DRAFT;

  @Column(nullable = false)
  private int quorumRequired;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id")
  private User createdBy;

  protected BoardMeeting() {}

  public BoardMeeting(
      Organization o,
      User u,
      String title,
      BoardMeetingType type,
      Instant start,
      Instant end,
      String location,
      String url,
      int quorum) {
    organization = o;
    createdBy = u;
    this.title = title;
    meetingType = type;
    startDateTime = start;
    endDateTime = end;
    this.location = clean(location);
    virtualMeetingUrl = clean(url);
    quorumRequired = quorum;
    validate();
  }

  public void update(
      String title,
      BoardMeetingType type,
      Instant start,
      Instant end,
      String location,
      String url,
      Integer quorum) {
    if (status == BoardMeetingStatus.COMPLETED || status == BoardMeetingStatus.CANCELLED)
      throw new BusinessRuleException(
          "TERMINAL_MEETING_IMMUTABLE", "Terminal board meetings cannot be edited");
    if (title != null) this.title = title.trim();
    if (type != null) meetingType = type;
    if (start != null) startDateTime = start;
    if (end != null) endDateTime = end;
    if (location != null) this.location = clean(location);
    if (url != null) virtualMeetingUrl = clean(url);
    if (quorum != null) quorumRequired = quorum;
    validate();
  }

  private void validate() {
    if (title == null
        || title.isBlank()
        || meetingType == null
        || startDateTime == null
        || endDateTime == null
        || !endDateTime.isAfter(startDateTime))
      throw new BusinessRuleException(
          "INVALID_BOARD_MEETING", "Meeting title, type, and valid dates are required");
    if (quorumRequired <= 0)
      throw new BusinessRuleException("INVALID_QUORUM", "Quorum requirement must be positive");
  }

  public void transition(BoardMeetingStatus target) {
    boolean ok =
        switch (status) {
          case DRAFT ->
              target == BoardMeetingStatus.PUBLISHED || target == BoardMeetingStatus.CANCELLED;
          case PUBLISHED ->
              target == BoardMeetingStatus.IN_PROGRESS || target == BoardMeetingStatus.CANCELLED;
          case IN_PROGRESS -> target == BoardMeetingStatus.COMPLETED;
          case COMPLETED, CANCELLED -> false;
        };
    if (!ok)
      throw new BusinessRuleException(
          "INVALID_MEETING_TRANSITION",
          "Meeting cannot transition from " + status + " to " + target);
    status = target;
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  public Organization getOrganization() {
    return organization;
  }

  public String getTitle() {
    return title;
  }

  public BoardMeetingType getMeetingType() {
    return meetingType;
  }

  public Instant getStartDateTime() {
    return startDateTime;
  }

  public Instant getEndDateTime() {
    return endDateTime;
  }

  public String getLocation() {
    return location;
  }

  public String getVirtualMeetingUrl() {
    return virtualMeetingUrl;
  }

  public BoardMeetingStatus getStatus() {
    return status;
  }

  public int getQuorumRequired() {
    return quorumRequired;
  }

  public User getCreatedBy() {
    return createdBy;
  }
}
