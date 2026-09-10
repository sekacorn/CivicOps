package org.civicops.foodpantry.pantry;

import java.util.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.foodpantry.pantry.dto.*;
import org.civicops.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoodPantryLocationService {
  private final FoodPantryLocationRepository locations;
  private final OrganizationService organizations;

  public FoodPantryLocationService(FoodPantryLocationRepository l, OrganizationService o) {
    locations = l;
    organizations = o;
  }

  @Transactional
  public PantryLocationResponse create(UUID org, CreatePantryLocationRequest r) {
    return PantryLocationResponse.from(
        locations.save(
            new FoodPantryLocation(
                organizations.requireEntity(org),
                r.name(),
                r.description(),
                r.addressLine1(),
                r.addressLine2(),
                r.city(),
                r.state(),
                r.postalCode(),
                r.country(),
                r.timezone(),
                r.notes())));
  }

  @Transactional
  public PantryLocationResponse update(UUID org, UUID id, UpdatePantryLocationRequest r) {
    FoodPantryLocation p = require(org, id);
    p.update(
        r.name(),
        r.description(),
        r.addressLine1(),
        r.addressLine2(),
        r.city(),
        r.state(),
        r.postalCode(),
        r.country(),
        r.timezone(),
        r.notes());
    return PantryLocationResponse.from(p);
  }

  @Transactional
  public PantryLocationResponse active(UUID org, UUID id, boolean active) {
    FoodPantryLocation p = require(org, id);
    if (active) p.activate();
    else p.deactivate();
    return PantryLocationResponse.from(p);
  }

  @Transactional(readOnly = true)
  public FoodPantryLocation require(UUID org, UUID id) {
    return locations
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Food pantry location", id));
  }

  @Transactional(readOnly = true)
  public PantryLocationResponse detail(UUID org, UUID id) {
    return PantryLocationResponse.from(require(org, id));
  }

  @Transactional(readOnly = true)
  public Page<PantryLocationResponse> list(UUID org, Boolean active, String city, Pageable page) {
    Specification<FoodPantryLocation> s =
        (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (active != null) s = s.and((r, q, c) -> c.equal(r.get("active"), active));
    if (city != null)
      s = s.and((r, q, c) -> c.equal(c.lower(r.get("city")), city.trim().toLowerCase(Locale.ROOT)));
    return locations.findAll(s, page).map(PantryLocationResponse::from);
  }
}
