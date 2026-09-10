package org.civicops.foodpantry.household;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.foodpantry.distribution.*;
import org.civicops.foodpantry.distribution.dto.DistributionVisitSummaryResponse;
import org.civicops.foodpantry.household.dto.*;
import org.civicops.foodpantry.security.FoodPantryAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/pantry-households")
@Tag(name = "Pantry Households")
public class PantryHouseholdController {
  private final PantryHouseholdService households;
  private final PantryDistributionService distributions;
  private final FoodPantryAccessService access;

  public PantryHouseholdController(
      PantryHouseholdService h, PantryDistributionService d, FoodPantryAccessService a) {
    households = h;
    distributions = d;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PantryHouseholdDetailResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreatePantryHouseholdRequest r) {
    access.requirePantryManagement(organizationId);
    return households.create(organizationId, r);
  }

  @GetMapping
  public Page<PantryHouseholdSummaryResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) String externalReferenceNumber,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String name,
      @PageableDefault(size = 20, sort = "createdAt") Pageable p) {
    access.requireInventoryRead(organizationId);
    if (email != null) access.requireHouseholdDetail(organizationId);
    return households.list(
        organizationId,
        active,
        externalReferenceNumber,
        email,
        name,
        SafePageables.allow(p, Set.of("createdAt", "householdName")));
  }

  @GetMapping("/{householdId}")
  public PantryHouseholdDetailResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID householdId) {
    access.requireHouseholdDetail(organizationId);
    return households.detail(organizationId, householdId);
  }

  @PatchMapping("/{householdId}")
  public PantryHouseholdDetailResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID householdId,
      @Valid @RequestBody UpdatePantryHouseholdRequest r) {
    access.requireHouseholdDetail(organizationId);
    return households.update(organizationId, householdId, r);
  }

  @PostMapping("/{householdId}/deactivate")
  public PantryHouseholdDetailResponse deactivate(
      @PathVariable UUID organizationId, @PathVariable UUID householdId) {
    access.requireHouseholdDetail(organizationId);
    return households.active(organizationId, householdId, false);
  }

  @GetMapping("/{householdId}/visits")
  public List<DistributionVisitSummaryResponse> visits(
      @PathVariable UUID organizationId, @PathVariable UUID householdId) {
    access.requireHouseholdDetail(organizationId);
    return distributions.householdVisits(organizationId, householdId);
  }
}
