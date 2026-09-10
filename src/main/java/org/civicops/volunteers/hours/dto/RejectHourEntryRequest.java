package org.civicops.volunteers.hours.dto;

import jakarta.validation.constraints.*;

public record RejectHourEntryRequest(@NotBlank @Size(max = 500) String reason) {}
