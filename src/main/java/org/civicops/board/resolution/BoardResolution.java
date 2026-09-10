package org.civicops.board.resolution;

import jakarta.persistence.*;
import java.time.*;
import org.civicops.board.meeting.BoardMeeting;
import org.civicops.board.motion.BoardMotion;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "board_resolution")
public class BoardResolution extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "meeting_id")
  private BoardMeeting meeting;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "motion_id")
  private BoardMotion motion;

  @Column(nullable = false)
  private String resolutionNumber;

  @Column(nullable = false)
  private String title;

  @Column(name = "resolution_text", nullable = false, columnDefinition = "TEXT")
  private String text;

  @Column(nullable = false)
  private LocalDate adoptedDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ResolutionStatus status = ResolutionStatus.ADOPTED;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id")
  private User createdBy;

  private Instant rescindedAt;

  protected BoardResolution() {}

  public BoardResolution(
      BoardMotion motion, String number, String title, String text, LocalDate date, User creator) {
    organization = motion.getOrganization();
    meeting = motion.getMeeting();
    this.motion = motion;
    resolutionNumber = required(number);
    this.title = required(title);
    this.text = required(text);
    adoptedDate = date;
    createdBy = creator;
    if (date == null)
      throw new BusinessRuleException("INVALID_RESOLUTION", "Adopted date is required");
  }

  public void rescind(Instant now) {
    if (status != ResolutionStatus.ADOPTED)
      throw new BusinessRuleException(
          "RESOLUTION_ALREADY_RESCINDED", "Resolution is already rescinded");
    status = ResolutionStatus.RESCINDED;
    rescindedAt = now;
  }

  private static String required(String s) {
    if (s == null || s.isBlank())
      throw new BusinessRuleException("INVALID_RESOLUTION", "Resolution fields are required");
    return s.trim();
  }

  public Organization getOrganization() {
    return organization;
  }

  public BoardMeeting getMeeting() {
    return meeting;
  }

  public BoardMotion getMotion() {
    return motion;
  }

  public String getResolutionNumber() {
    return resolutionNumber;
  }

  public String getTitle() {
    return title;
  }

  public String getText() {
    return text;
  }

  public LocalDate getAdoptedDate() {
    return adoptedDate;
  }

  public ResolutionStatus getStatus() {
    return status;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public Instant getRescindedAt() {
    return rescindedAt;
  }
}
