package org.civicops.donations.donation.dto;

import jakarta.validation.constraints.*;

public record ReverseDonationRequest(@NotBlank @Size(max = 500) String reason) {}
