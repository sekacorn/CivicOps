package org.civicops.foodpantry.distribution;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.civicops.foodpantry.distribution.dto.*;
import org.civicops.foodpantry.security.FoodPantryAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
@Tag(name = "Pantry Distributions")
public class PantryDistributionController {
  private final PantryDistributionService distributions;
  private final FoodPantryAccessService access;

  public PantryDistributionController(PantryDistributionService d, FoodPantryAccessService a) {
    distributions = d;
    access = a;
  }

  @PostMapping("/food-pantries/{pantryId}/distribution-visits")
  @ResponseStatus(HttpStatus.CREATED)
  public DistributionVisitDetailResponse create(
      @PathVariable UUID organizationId,
      @PathVariable UUID pantryId,
      @Valid @RequestBody CreateDistributionVisitRequest r) {
    access.requireDistributionManagement(organizationId);
    return distributions.create(organizationId, pantryId, access.userId(), r);
  }

  @GetMapping("/food-pantries/{pantryId}/distribution-visits")
  public Page<DistributionVisitSummaryResponse> list(
      @PathVariable UUID organizationId,
      @PathVariable UUID pantryId,
      @RequestParam(required = false) UUID householdId,
      @RequestParam(required = false) DistributionVisitStatus status,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @PageableDefault(size = 20, sort = "visitDateTime") Pageable p) {
    access.requireDistributionManagement(organizationId);
    return distributions.list(
        organizationId,
        pantryId,
        householdId,
        status,
        from,
        to,
        SafePageables.allow(p, Set.of("visitDateTime", "createdAt")));
  }

  @GetMapping("/pantry-distribution-visits/{visitId}")
  public DistributionVisitDetailResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID visitId) {
    access.requireDistributionManagement(organizationId);
    return distributions.detail(organizationId, visitId);
  }

  @PostMapping("/pantry-distribution-visits/{visitId}/items")
  public DistributionVisitDetailResponse item(
      @PathVariable UUID organizationId,
      @PathVariable UUID visitId,
      @Valid @RequestBody AddDistributionItemRequest r) {
    access.requireDistributionManagement(organizationId);
    return distributions.addItem(organizationId, visitId, r);
  }

  @PostMapping("/pantry-distribution-visits/{visitId}/complete")
  public DistributionVisitDetailResponse complete(
      @PathVariable UUID organizationId, @PathVariable UUID visitId) {
    access.requireDistributionManagement(organizationId);
    return distributions.complete(organizationId, visitId, access.userId());
  }

  @PostMapping("/pantry-distribution-visits/{visitId}/cancel")
  public DistributionVisitDetailResponse cancel(
      @PathVariable UUID organizationId, @PathVariable UUID visitId) {
    access.requireDistributionManagement(organizationId);
    return distributions.cancel(organizationId, visitId);
  }
}
