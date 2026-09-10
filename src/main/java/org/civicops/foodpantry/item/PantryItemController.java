package org.civicops.foodpantry.item;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.foodpantry.item.dto.*;
import org.civicops.foodpantry.security.FoodPantryAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/pantry-items")
@Tag(name = "Pantry Items")
public class PantryItemController {
  private final PantryItemService items;
  private final FoodPantryAccessService access;

  public PantryItemController(PantryItemService i, FoodPantryAccessService a) {
    items = i;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PantryItemResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreatePantryItemRequest r) {
    access.requirePantryManagement(organizationId);
    return items.create(organizationId, r);
  }

  @GetMapping
  public Page<PantryItemResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) FoodCategory category,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String sku,
      @PageableDefault(size = 20, sort = "name") Pageable p) {
    access.requireInventoryRead(organizationId);
    return items.list(
        organizationId,
        category,
        active,
        name,
        sku,
        SafePageables.allow(p, Set.of("name", "category", "createdAt")));
  }

  @GetMapping("/{itemId}")
  public PantryItemResponse detail(@PathVariable UUID organizationId, @PathVariable UUID itemId) {
    access.requireInventoryRead(organizationId);
    return items.detail(organizationId, itemId);
  }

  @PatchMapping("/{itemId}")
  public PantryItemResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID itemId,
      @Valid @RequestBody UpdatePantryItemRequest r) {
    access.requirePantryManagement(organizationId);
    return items.update(organizationId, itemId, r);
  }

  @PostMapping("/{itemId}/deactivate")
  public PantryItemResponse deactivate(
      @PathVariable UUID organizationId, @PathVariable UUID itemId) {
    access.requirePantryManagement(organizationId);
    return items.active(organizationId, itemId, false);
  }

  @PostMapping("/{itemId}/activate")
  public PantryItemResponse activate(@PathVariable UUID organizationId, @PathVariable UUID itemId) {
    access.requirePantryManagement(organizationId);
    return items.active(organizationId, itemId, true);
  }
}
