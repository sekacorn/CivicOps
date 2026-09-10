package org.civicops.facilities.facility;

import java.time.ZoneId;
import java.util.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.facilities.facility.dto.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacilityService {
  private final FacilityRepository facilities;
  private final OrganizationService organizations;

  public FacilityService(FacilityRepository f, OrganizationService o) {
    facilities = f;
    organizations = o;
  }

  @Transactional
  public FacilityResponse create(UUID org, CreateFacilityRequest r) {
    zone(r.timezone());
    return FacilityResponse.from(
        facilities.save(
            new Facility(
                organizations.requireEntity(org),
                r.name().trim(),
                clean(r.description()),
                r.facilityType(),
                clean(r.addressLine1()),
                clean(r.addressLine2()),
                clean(r.city()),
                clean(r.state()),
                clean(r.postalCode()),
                upper(r.country()),
                r.timezone(),
                clean(r.notes()))));
  }

  @Transactional(readOnly = true)
  public Facility require(UUID org, UUID id) {
    return facilities
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Facility", id));
  }

  @Transactional(readOnly = true)
  public FacilityResponse detail(UUID org, UUID id) {
    return FacilityResponse.from(require(org, id));
  }

  @Transactional
  public FacilityResponse update(UUID org, UUID id, UpdateFacilityRequest r) {
    if (r.timezone() != null) zone(r.timezone());
    Facility f = require(org, id);
    f.update(
        clean(r.name()),
        clean(r.description()),
        r.facilityType(),
        clean(r.addressLine1()),
        clean(r.addressLine2()),
        clean(r.city()),
        clean(r.state()),
        clean(r.postalCode()),
        upper(r.country()),
        r.timezone(),
        clean(r.notes()),
        r.active());
    return FacilityResponse.from(f);
  }

  @Transactional(readOnly = true)
  public Page<FacilityResponse> list(
      UUID org, Boolean active, FacilityType type, String city, Pageable p) {
    Specification<Facility> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (active != null) s = s.and((r, q, c) -> c.equal(r.get("active"), active));
    if (type != null) s = s.and((r, q, c) -> c.equal(r.get("facilityType"), type));
    if (city != null)
      s = s.and((r, q, c) -> c.equal(c.lower(r.get("city")), city.trim().toLowerCase(Locale.ROOT)));
    return facilities.findAll(s, p).map(FacilityResponse::from);
  }

  private static void zone(String z) {
    try {
      ZoneId.of(z);
    } catch (Exception e) {
      throw new BusinessRuleException(
          "INVALID_TIMEZONE", "Facility timezone must be a valid IANA timezone");
    }
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  private static String upper(String s) {
    String x = clean(s);
    return x == null ? null : x.toUpperCase(Locale.ROOT);
  }
}
