package org.civicops.cases.client.dto;

import java.time.*;
import java.util.UUID;
import org.civicops.cases.client.Client;

public record ClientDetailResponse(
    UUID id,
    UUID organizationId,
    String externalReferenceNumber,
    String firstName,
    String lastName,
    String preferredName,
    LocalDate dateOfBirth,
    String email,
    String phone,
    String addressLine1,
    String addressLine2,
    String city,
    String state,
    String postalCode,
    String country,
    String preferredContactMethod,
    boolean active,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static ClientDetailResponse from(Client c) {
    return new ClientDetailResponse(
        c.getId(),
        c.getOrganization().getId(),
        c.getExternalReferenceNumber(),
        c.getFirstName(),
        c.getLastName(),
        c.getPreferredName(),
        c.getDateOfBirth(),
        c.getEmail(),
        c.getPhone(),
        c.getAddressLine1(),
        c.getAddressLine2(),
        c.getCity(),
        c.getState(),
        c.getPostalCode(),
        c.getCountry(),
        c.getPreferredContactMethod(),
        c.isActive(),
        c.getCreatedAt(),
        c.getUpdatedAt(),
        c.getVersion());
  }
}
