package org.civicops.facilities.reservation.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public record CreateReservationRequest(
    @NotNull UUID facilitySpaceId,
    UUID requestedByUserId,
    @Size(max = 200) String requesterName,
    @Email @Size(max = 320) String requesterEmail,
    UUID eventId,
    @NotBlank @Size(max = 200) String title,
    @Size(max = 10000) String purpose,
    @NotNull Instant startDateTime,
    @NotNull Instant endDateTime,
    @Positive Integer expectedAttendance,
    @Size(max = 10000) String notes) {}
