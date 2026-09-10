package org.civicops.facilities.facility.dto;

import jakarta.validation.constraints.*;
import org.civicops.facilities.facility.FacilityType;

public record CreateFacilityRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 10000) String description,
    @NotNull FacilityType facilityType,
    @Size(max = 250) String addressLine1,
    @Size(max = 250) String addressLine2,
    @Size(max = 120) String city,
    @Size(max = 120) String state,
    @Size(max = 30) String postalCode,
    @Pattern(regexp = "^[A-Za-z]{2}$") String country,
    @NotBlank @Size(max = 80) String timezone,
    @Size(max = 10000) String notes) {}
