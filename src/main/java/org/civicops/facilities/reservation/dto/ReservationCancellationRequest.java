package org.civicops.facilities.reservation.dto;

import jakarta.validation.constraints.Size;

public record ReservationCancellationRequest(@Size(max = 2000) String reason) {}
