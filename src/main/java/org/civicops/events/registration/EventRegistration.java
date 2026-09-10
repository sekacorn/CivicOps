package org.civicops.events.registration;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.events.event.EventRecord;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "event_registration")
public class EventRegistration extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  private EventRecord event;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "registered_user_id")
  private User registeredUser;

  @Column(nullable = false, length = 200)
  private String attendeeName;

  @Column(length = 320)
  private String attendeeEmail;

  @Column(length = 50)
  private String attendeePhone;

  @Column(nullable = false)
  private Instant registrationDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private RegistrationStatus status;

  private Instant checkedInAt;
  private Instant checkedOutAt;

  protected EventRegistration() {}

  public EventRegistration(
      Organization org,
      EventRecord event,
      User user,
      String name,
      String email,
      String phone,
      RegistrationStatus status,
      Instant now) {
    this.organization = org;
    this.event = event;
    this.registeredUser = user;
    this.attendeeName = name;
    this.attendeeEmail = email;
    this.attendeePhone = phone;
    this.status = status;
    this.registrationDate = now;
  }

  public Organization getOrganization() {
    return organization;
  }

  public EventRecord getEvent() {
    return event;
  }

  public User getRegisteredUser() {
    return registeredUser;
  }

  public String getAttendeeName() {
    return attendeeName;
  }

  public String getAttendeeEmail() {
    return attendeeEmail;
  }

  public String getAttendeePhone() {
    return attendeePhone;
  }

  public Instant getRegistrationDate() {
    return registrationDate;
  }

  public RegistrationStatus getStatus() {
    return status;
  }

  public Instant getCheckedInAt() {
    return checkedInAt;
  }

  public Instant getCheckedOutAt() {
    return checkedOutAt;
  }

  public boolean active() {
    return status == RegistrationStatus.REGISTERED
        || status == RegistrationStatus.WAITLISTED
        || status == RegistrationStatus.ATTENDED;
  }

  public void cancel() {
    if (status == RegistrationStatus.CANCELLED)
      throw new BusinessRuleException(
          "REGISTRATION_ALREADY_CANCELLED", "Registration is already cancelled");
    if (status == RegistrationStatus.ATTENDED)
      throw new BusinessRuleException(
          "ATTENDED_REGISTRATION_NOT_CANCELLABLE", "Attended registrations cannot be cancelled");
    status = RegistrationStatus.CANCELLED;
  }

  public void promote() {
    if (status != RegistrationStatus.WAITLISTED)
      throw new BusinessRuleException(
          "REGISTRATION_NOT_WAITLISTED", "Only waitlisted registrations can be promoted");
    status = RegistrationStatus.REGISTERED;
  }

  public void checkIn(Instant now) {
    if (status != RegistrationStatus.REGISTERED)
      throw new BusinessRuleException(
          "REGISTRATION_NOT_CHECKIN_ELIGIBLE", "Only registered attendees can check in");
    if (checkedInAt != null)
      throw new BusinessRuleException("ALREADY_CHECKED_IN", "Attendee is already checked in");
    checkedInAt = now;
    status = RegistrationStatus.ATTENDED;
  }

  public void checkOut(Instant now) {
    if (checkedInAt == null)
      throw new BusinessRuleException(
          "CHECK_OUT_REQUIRES_CHECK_IN", "Attendee must check in before checking out");
    if (checkedOutAt != null)
      throw new BusinessRuleException("ALREADY_CHECKED_OUT", "Attendee is already checked out");
    checkedOutAt = now;
  }
}
