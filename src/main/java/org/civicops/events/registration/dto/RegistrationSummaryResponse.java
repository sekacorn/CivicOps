package org.civicops.events.registration.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.events.registration.*;

public record RegistrationSummaryResponse(
    UUID id, String attendeeName, RegistrationStatus status, Instant registrationDate) {
  public static RegistrationSummaryResponse from(EventRegistration r) {
    return new RegistrationSummaryResponse(
        r.getId(), r.getAttendeeName(), r.getStatus(), r.getRegistrationDate());
  }
}
