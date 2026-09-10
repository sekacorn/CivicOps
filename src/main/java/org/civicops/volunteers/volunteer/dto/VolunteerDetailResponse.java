package org.civicops.volunteers.volunteer.dto;

import java.time.*;
import java.util.*;
import org.civicops.volunteers.volunteer.*;

public record VolunteerDetailResponse(
    UUID id,
    UUID organizationId,
    UUID userId,
    String firstName,
    String lastName,
    String email,
    String phone,
    String addressLine1,
    String addressLine2,
    String city,
    String state,
    String postalCode,
    String country,
    String emergencyContactName,
    String emergencyContactPhone,
    String notes,
    VolunteerStatus status,
    LocalDate startDate,
    Set<String> skills,
    Instant createdAt,
    Instant updatedAt) {
  public static VolunteerDetailResponse from(Volunteer v) {
    return new VolunteerDetailResponse(
        v.getId(),
        v.getOrganization().getId(),
        v.getUser() == null ? null : v.getUser().getId(),
        v.getFirstName(),
        v.getLastName(),
        v.getEmail(),
        v.getPhone(),
        v.getAddressLine1(),
        v.getAddressLine2(),
        v.getCity(),
        v.getState(),
        v.getPostalCode(),
        v.getCountry(),
        v.getEmergencyContactName(),
        v.getEmergencyContactPhone(),
        v.getNotes(),
        v.getStatus(),
        v.getStartDate(),
        v.getSkills(),
        v.getCreatedAt(),
        v.getUpdatedAt());
  }
}
