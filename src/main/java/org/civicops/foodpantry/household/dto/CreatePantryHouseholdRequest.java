package org.civicops.foodpantry.household.dto;

import jakarta.validation.constraints.*;

public record CreatePantryHouseholdRequest(
    @Size(max = 100) String externalReferenceNumber,
    @Size(max = 200) String householdName,
    @Size(max = 100) String primaryContactFirstName,
    @Size(max = 100) String primaryContactLastName,
    @Email @Size(max = 320) String email,
    @Size(max = 50) String phone,
    @Size(max = 1000) String address,
    @Min(1) int householdSize,
    @Size(max = 5000) String notes) {}
