package org.civicops.core.organization;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.civicops.core.organization.dto.CreateOrganizationRequest;
import org.civicops.core.organization.dto.OrganizationResponse;
import org.civicops.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {
  private final OrganizationRepository organizations;

  public OrganizationService(OrganizationRepository organizations) {
    this.organizations = organizations;
  }

  @Transactional
  public OrganizationResponse create(CreateOrganizationRequest request) {
    Organization organization =
        new Organization(
            request.name().trim(),
            clean(request.legalName()),
            clean(request.description()),
            request.organizationType(),
            normalizeEmail(request.email()),
            clean(request.phone()),
            clean(request.website()),
            clean(request.addressLine1()),
            clean(request.addressLine2()),
            clean(request.city()),
            clean(request.state()),
            clean(request.postalCode()),
            normalizeCountry(request.country()));
    return OrganizationResponse.from(organizations.save(organization));
  }

  @Transactional(readOnly = true)
  public OrganizationResponse get(UUID id) {
    return OrganizationResponse.from(requireEntity(id));
  }

  @Transactional(readOnly = true)
  public List<OrganizationResponse> list() {
    return organizations.findAll().stream().map(OrganizationResponse::from).toList();
  }

  public Organization requireEntity(UUID id) {
    return organizations
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Organization", id));
  }

  private static String normalizeEmail(String value) {
    String cleaned = clean(value);
    return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
  }

  private static String normalizeCountry(String value) {
    String cleaned = clean(value);
    return cleaned == null ? null : cleaned.toUpperCase(Locale.ROOT);
  }

  private static String clean(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
  }
}
