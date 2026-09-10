package org.civicops.donations.donor.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.donations.donor.*;

public record DonorDetailResponse(
    UUID id,
    UUID organizationId,
    DonorType donorType,
    String firstName,
    String lastName,
    String organizationName,
    String email,
    String phone,
    String addressLine1,
    String addressLine2,
    String city,
    String state,
    String postalCode,
    String country,
    boolean anonymous,
    boolean communicationOptOut,
    String notes,
    Instant createdAt,
    Instant updatedAt) {
  public static DonorDetailResponse from(Donor d) {
    return new DonorDetailResponse(
        d.getId(),
        d.getOrganization().getId(),
        d.getDonorType(),
        d.getFirstName(),
        d.getLastName(),
        d.getOrganizationName(),
        d.getEmail(),
        d.getPhone(),
        d.getAddressLine1(),
        d.getAddressLine2(),
        d.getCity(),
        d.getState(),
        d.getPostalCode(),
        d.getCountry(),
        d.isAnonymous(),
        d.isCommunicationOptOut(),
        d.getNotes(),
        d.getCreatedAt(),
        d.getUpdatedAt());
  }
}
