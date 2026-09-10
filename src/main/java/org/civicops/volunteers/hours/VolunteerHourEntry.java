package org.civicops.volunteers.hours;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;
import org.civicops.volunteers.assignment.VolunteerAssignment;
import org.civicops.volunteers.volunteer.Volunteer;

@Entity
@Table(name = "volunteer_hour_entry")
public class VolunteerHourEntry extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "volunteer_id")
  private Volunteer volunteer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignment_id")
  private VolunteerAssignment assignment;

  @Column(nullable = false)
  private LocalDate serviceDate;

  @Column(nullable = false, precision = 5, scale = 2)
  private BigDecimal hours;

  @Column(length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private HourEntryStatus status = HourEntryStatus.SUBMITTED;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by_user_id")
  private User reviewedByUser;

  private Instant reviewedAt;

  @Column(length = 500)
  private String rejectionReason;

  protected VolunteerHourEntry() {}

  public VolunteerHourEntry(
      Organization o, Volunteer v, VolunteerAssignment a, LocalDate d, BigDecimal h, String x) {
    organization = o;
    volunteer = v;
    assignment = a;
    serviceDate = d;
    hours = h;
    description = x;
  }

  public Organization getOrganization() {
    return organization;
  }

  public Volunteer getVolunteer() {
    return volunteer;
  }

  public VolunteerAssignment getAssignment() {
    return assignment;
  }

  public LocalDate getServiceDate() {
    return serviceDate;
  }

  public BigDecimal getHours() {
    return hours;
  }

  public String getDescription() {
    return description;
  }

  public HourEntryStatus getStatus() {
    return status;
  }

  public User getReviewedByUser() {
    return reviewedByUser;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public void approve(User reviewer, Instant now) {
    ensureSubmitted();
    status = HourEntryStatus.APPROVED;
    reviewedByUser = reviewer;
    reviewedAt = now;
  }

  public void reject(User reviewer, Instant now, String reason) {
    ensureSubmitted();
    status = HourEntryStatus.REJECTED;
    reviewedByUser = reviewer;
    reviewedAt = now;
    rejectionReason = reason;
  }

  private void ensureSubmitted() {
    if (status != HourEntryStatus.SUBMITTED)
      throw new BusinessRuleException(
          "HOURS_ALREADY_REVIEWED", "Only submitted hours can be reviewed");
  }
}
