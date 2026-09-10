package org.civicops.facilities.space.dto;

import jakarta.validation.constraints.*;

public record UpdateFacilitySpaceRequest(
    @Size(max = 200) String name,
    @Size(max = 10000) String description,
    @Positive Integer capacity,
    Boolean reservable,
    Boolean active,
    @Size(max = 500) String locationDetails,
    @Size(max = 10000) String accessibilityNotes) {}
