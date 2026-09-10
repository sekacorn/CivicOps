package org.civicops.foodpantry.household;

import java.util.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.foodpantry.household.dto.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PantryHouseholdService {
  private final PantryHouseholdRepository households;
  private final OrganizationService organizations;

  public PantryHouseholdService(PantryHouseholdRepository h, OrganizationService o) {
    households = h;
    organizations = o;
  }

  @Transactional
  public PantryHouseholdDetailResponse create(UUID org, CreatePantryHouseholdRequest r) {
    duplicate(org, null, r.externalReferenceNumber());
    return PantryHouseholdDetailResponse.from(
        households.save(
            new PantryHousehold(
                organizations.requireEntity(org),
                r.externalReferenceNumber(),
                r.householdName(),
                r.primaryContactFirstName(),
                r.primaryContactLastName(),
                r.email(),
                r.phone(),
                r.address(),
                r.householdSize(),
                r.notes())));
  }

  @Transactional
  public PantryHouseholdDetailResponse update(UUID org, UUID id, UpdatePantryHouseholdRequest r) {
    PantryHousehold h = require(org, id);
    duplicate(org, h.getExternalReferenceNumber(), r.externalReferenceNumber());
    h.update(
        r.externalReferenceNumber(),
        r.householdName(),
        r.primaryContactFirstName(),
        r.primaryContactLastName(),
        r.email(),
        r.phone(),
        r.address(),
        r.householdSize(),
        r.notes());
    return PantryHouseholdDetailResponse.from(h);
  }

  @Transactional
  public PantryHouseholdDetailResponse active(UUID org, UUID id, boolean active) {
    PantryHousehold h = require(org, id);
    if (active) h.activate();
    else h.deactivate();
    return PantryHouseholdDetailResponse.from(h);
  }

  @Transactional(readOnly = true)
  public PantryHousehold require(UUID org, UUID id) {
    return households
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Pantry household", id));
  }

  @Transactional(readOnly = true)
  public PantryHouseholdDetailResponse detail(UUID org, UUID id) {
    return PantryHouseholdDetailResponse.from(require(org, id));
  }

  @Transactional(readOnly = true)
  public Page<PantryHouseholdSummaryResponse> list(
      UUID org, Boolean active, String reference, String email, String name, Pageable page) {
    Specification<PantryHousehold> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (active != null) s = s.and((r, q, c) -> c.equal(r.get("active"), active));
    if (reference != null)
      s = s.and((r, q, c) -> c.equal(r.get("externalReferenceNumber"), reference.trim()));
    if (email != null)
      s =
          s.and(
              (r, q, c) ->
                  c.equal(r.get("normalizedEmail"), PantryHousehold.normalizeEmail(email)));
    if (name != null)
      s =
          s.and(
              (r, q, c) ->
                  c.like(
                      c.lower(r.get("householdName")),
                      "%" + name.trim().toLowerCase(Locale.ROOT) + "%"));
    return households.findAll(s, page).map(PantryHouseholdSummaryResponse::from);
  }

  private void duplicate(UUID org, String current, String next) {
    String n = next == null ? null : next.trim();
    if (n != null
        && !n.equals(current)
        && households.existsByOrganizationIdAndExternalReferenceNumber(org, n))
      throw new ConflictException(
          "DUPLICATE_HOUSEHOLD_REFERENCE", "Household reference already exists");
  }
}
