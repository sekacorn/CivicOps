package org.civicops.volunteers.volunteer.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.civicops.volunteers.volunteer.VolunteerStatus;

public record CreateVolunteerRequest(
    UUID userId,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email @Size(max = 320) String email,
    @Size(max = 50) String phone,
    @Size(max = 250) String addressLine1,
    @Size(max = 250) String addressLine2,
    @Size(max = 120) String city,
    @Size(max = 120) String state,
    @Size(max = 30) String postalCode,
    @Size(min = 2, max = 2) String country,
    @Size(max = 200) String emergencyContactName,
    @Size(max = 50) String emergencyContactPhone,
    @Size(max = 5000) String notes,
    VolunteerStatus status,
    @PastOrPresent LocalDate startDate,
    @Size(max = 50) Set<@NotBlank @Size(max = 100) String> skills) {}
