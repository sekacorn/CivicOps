package org.civicops.foodpantry.pantry.dto;

import jakarta.validation.constraints.*;

public record CreatePantryLocationRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 5000) String description,
    @Size(max = 250) String addressLine1,
    @Size(max = 250) String addressLine2,
    @Size(max = 120) String city,
    @Size(max = 120) String state,
    @Size(max = 30) String postalCode,
    @Pattern(regexp = "^[A-Za-z]{2}$") String country,
    @NotBlank @Size(max = 100) String timezone,
    @Size(max = 5000) String notes) {}
