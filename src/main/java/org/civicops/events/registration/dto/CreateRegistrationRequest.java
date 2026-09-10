package org.civicops.events.registration.dto;

import jakarta.validation.constraints.*;

public record CreateRegistrationRequest(
    @Size(max = 200) String attendeeName,
    @Email @Size(max = 320) String attendeeEmail,
    @Size(max = 50) String attendeePhone) {}
