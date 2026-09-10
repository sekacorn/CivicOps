package org.civicops.facilities.reservation;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.events.event.EventRecord;
import org.civicops.facilities.space.FacilitySpace;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "facility_reservation")
public class FacilityReservation extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "facility_space_id", nullable = false)
  private FacilitySpace facilitySpace;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requested_by_user_id")
  private User requestedByUser;

  private String requesterName, requesterEmail;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id")
  private EventRecord event;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String purpose;

  @Column(nullable = false)
  private Instant startDateTime, endDateTime;
  private Integer expectedAttendance;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReservationStatus status = ReservationStatus.PENDING;

  @Column(nullable = false)
  private Instant requestedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_by_user_id")
  private User approvedBy;

  private Instant approvedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "rejected_by_user_id")
  private User rejectedBy;

  private Instant rejectedAt;
  private String rejectionReason;
  private Instant cancelledAt;
  private String cancellationReason;

  @Column(columnDefinition = "TEXT")
  private String notes;

  protected FacilityReservation() {}

  public FacilityReservation(
      FacilitySpace s,
      User u,
      String n,
      String email,
      EventRecord event,
      String title,
      String purpose,
      Instant start,
      Instant end,
      Integer attendance,
      Instant requested,
      String notes) {
    organization = s.getOrganization();
    facilitySpace = s;
    requestedByUser = u;
    requesterName = n;
    requesterEmail = email;
    this.event = event;
    this.title = title;
    this.purpose = purpose;
    startDateTime = start;
    endDateTime = end;
    expectedAttendance = attendance;
    requestedAt = requested;
    this.notes = notes;
  }

  public Organization getOrganization() {
    return organization;
  }

  public FacilitySpace getFacilitySpace() {
    return facilitySpace;
  }

  public User getRequestedByUser() {
    return requestedByUser;
  }

  public String getRequesterName() {
    return requesterName;
  }

  public String getRequesterEmail() {
    return requesterEmail;
  }

  public EventRecord getEvent() {
    return event;
  }

  public String getTitle() {
    return title;
  }

  public String getPurpose() {
    return purpose;
  }

  public Instant getStartDateTime() {
    return startDateTime;
  }

  public Instant getEndDateTime() {
    return endDateTime;
  }

  public Integer getExpectedAttendance() {
    return expectedAttendance;
  }

  public ReservationStatus getStatus() {
    return status;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public User getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public User getRejectedBy() {
    return rejectedBy;
  }

  public Instant getRejectedAt() {
    return rejectedAt;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public String getNotes() {
    return notes;
  }

  public void update(
      String title, String purpose, Instant start, Instant end, Integer attendance, String notes) {
    if (status != ReservationStatus.PENDING)
      throw new BusinessRuleException(
          "RESERVATION_NOT_EDITABLE", "Only pending reservations can be edited");
    if (title != null) this.title = title;
    if (purpose != null) this.purpose = purpose;
    if (start != null) startDateTime = start;
    if (end != null) endDateTime = end;
    if (attendance != null) expectedAttendance = attendance;
    if (notes != null) this.notes = notes;
  }

  public void approve(User u, Instant now) {
    pending();
    status = ReservationStatus.APPROVED;
    approvedBy = u;
    approvedAt = now;
  }

  public void reject(User u, Instant now, String reason) {
    pending();
    status = ReservationStatus.REJECTED;
    rejectedBy = u;
    rejectedAt = now;
    rejectionReason = reason;
  }

  public void cancel(Instant now, String reason) {
    if (status != ReservationStatus.PENDING && status != ReservationStatus.APPROVED)
      throw new BusinessRuleException(
          "INVALID_RESERVATION_TRANSITION",
          "Only pending or approved reservations can be cancelled");
    status = ReservationStatus.CANCELLED;
    cancelledAt = now;
    cancellationReason = reason;
  }

  public void complete(Instant now) {
    if (status != ReservationStatus.APPROVED)
      throw new BusinessRuleException(
          "INVALID_RESERVATION_TRANSITION", "Only approved reservations can be completed");
    if (now.isBefore(endDateTime))
      throw new BusinessRuleException(
          "FUTURE_RESERVATION_NOT_COMPLETABLE", "Future reservations cannot be completed");
    status = ReservationStatus.COMPLETED;
  }

  private void pending() {
    if (status != ReservationStatus.PENDING)
      throw new BusinessRuleException(
          "INVALID_RESERVATION_TRANSITION", "Reservation must be pending");
  }
}
