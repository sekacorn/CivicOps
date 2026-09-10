package org.civicops.facilities.reservation.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record UpdateReservationRequest(
    @Size(max = 200) String title,
    @Size(max = 10000) String purpose,
    Instant startDateTime,
    Instant endDateTime,
    @Positive Integer expectedAttendance,
    @Size(max = 10000) String notes) {}
