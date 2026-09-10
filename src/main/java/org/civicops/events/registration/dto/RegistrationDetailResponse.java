package org.civicops.events.registration.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.events.registration.*;

public record RegistrationDetailResponse(
    UUID id,
    UUID eventId,
    UUID organizationId,
    UUID registeredUserId,
    String attendeeName,
    String attendeeEmail,
    String attendeePhone,
    RegistrationStatus status,
    Instant registrationDate,
    Instant checkedInAt,
    Instant checkedOutAt) {
  public static RegistrationDetailResponse from(EventRegistration r) {
    return new RegistrationDetailResponse(
        r.getId(),
        r.getEvent().getId(),
        r.getOrganization().getId(),
        r.getRegisteredUser() == null ? null : r.getRegisteredUser().getId(),
        r.getAttendeeName(),
        r.getAttendeeEmail(),
        r.getAttendeePhone(),
        r.getStatus(),
        r.getRegistrationDate(),
        r.getCheckedInAt(),
        r.getCheckedOutAt());
  }
}
