package org.civicops.donations.donor.dto;

import jakarta.validation.constraints.*;
import org.civicops.donations.donor.DonorType;

public record CreateDonorRequest(
    @NotNull DonorType donorType,
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Size(max = 200) String organizationName,
    @Email @Size(max = 320) String email,
    @Size(max = 50) String phone,
    @Size(max = 250) String addressLine1,
    @Size(max = 250) String addressLine2,
    @Size(max = 120) String city,
    @Size(max = 120) String state,
    @Size(max = 30) String postalCode,
    @Size(max = 2) String country,
    boolean anonymous,
    boolean communicationOptOut,
    String notes) {}
