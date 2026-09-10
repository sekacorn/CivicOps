package org.civicops.core.organization.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.core.organization.Organization;
import org.civicops.core.organization.OrganizationType;

public record OrganizationResponse(
    UUID id,
    String name,
    String legalName,
    String description,
    OrganizationType organizationType,
    String email,
    String phone,
    String website,
    String addressLine1,
    String addressLine2,
    String city,
    String state,
    String postalCode,
    String country,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {
  public static OrganizationResponse from(Organization organization) {
    return new OrganizationResponse(
        organization.getId(),
        organization.getName(),
        organization.getLegalName(),
        organization.getDescription(),
        organization.getOrganizationType(),
        organization.getEmail(),
        organization.getPhone(),
        organization.getWebsite(),
        organization.getAddressLine1(),
        organization.getAddressLine2(),
        organization.getCity(),
        organization.getState(),
        organization.getPostalCode(),
        organization.getCountry(),
        organization.isActive(),
        organization.getCreatedAt(),
        organization.getUpdatedAt());
  }
}
