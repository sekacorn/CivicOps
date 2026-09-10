package org.civicops.foodpantry.pantry;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.foodpantry.pantry.dto.*;
import org.civicops.foodpantry.security.FoodPantryAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/food-pantries")
@Tag(name = "Food Pantry Locations")
public class FoodPantryLocationController {
  private final FoodPantryLocationService locations;
  private final FoodPantryAccessService access;

  public FoodPantryLocationController(FoodPantryLocationService l, FoodPantryAccessService a) {
    locations = l;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PantryLocationResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreatePantryLocationRequest r) {
    access.requirePantryManagement(organizationId);
    return locations.create(organizationId, r);
  }

  @GetMapping
  public Page<PantryLocationResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) String city,
      @PageableDefault(size = 20, sort = "name") Pageable p) {
    access.requireInventoryRead(organizationId);
    return locations.list(
        organizationId, active, city, SafePageables.allow(p, Set.of("name", "city", "createdAt")));
  }

  @GetMapping("/{pantryId}")
  public PantryLocationResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID pantryId) {
    access.requireInventoryRead(organizationId);
    return locations.detail(organizationId, pantryId);
  }

  @PatchMapping("/{pantryId}")
  public PantryLocationResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID pantryId,
      @Valid @RequestBody UpdatePantryLocationRequest r) {
    access.requirePantryManagement(organizationId);
    return locations.update(organizationId, pantryId, r);
  }

  @PostMapping("/{pantryId}/deactivate")
  public PantryLocationResponse deactivate(
      @PathVariable UUID organizationId, @PathVariable UUID pantryId) {
    access.requirePantryManagement(organizationId);
    return locations.active(organizationId, pantryId, false);
  }

  @PostMapping("/{pantryId}/activate")
  public PantryLocationResponse activate(
      @PathVariable UUID organizationId, @PathVariable UUID pantryId) {
    access.requirePantryManagement(organizationId);
    return locations.active(organizationId, pantryId, true);
  }
}
