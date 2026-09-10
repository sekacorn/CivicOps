package org.civicops.facilities.reservation.dto;

import jakarta.validation.constraints.*;

public record ReservationDecisionRequest(@NotBlank @Size(max = 2000) String reason) {}
