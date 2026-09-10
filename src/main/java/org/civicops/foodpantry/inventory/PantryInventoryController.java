package org.civicops.foodpantry.inventory;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.foodpantry.inventory.dto.*;
import org.civicops.foodpantry.security.FoodPantryAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
@Tag(name = "Pantry Inventory")
public class PantryInventoryController {
  private final PantryInventoryService inventory;
  private final FoodPantryAccessService access;

  public PantryInventoryController(PantryInventoryService i, FoodPantryAccessService a) {
    inventory = i;
    access = a;
  }

  @PostMapping("/food-pantries/{pantryId}/inventory-receipts")
  @ResponseStatus(HttpStatus.CREATED)
  public InventoryLotResponse receive(
      @PathVariable UUID organizationId,
      @PathVariable UUID pantryId,
      @Valid @RequestBody CreateInventoryReceiptRequest r) {
    access.requirePantryManagement(organizationId);
    return inventory.receive(organizationId, pantryId, access.userId(), r);
  }

  @GetMapping("/food-pantries/{pantryId}/inventory")
  public Page<InventoryLotResponse> list(
      @PathVariable UUID organizationId,
      @PathVariable UUID pantryId,
      @RequestParam(required = false) UUID itemId,
      @RequestParam(required = false) Boolean expired,
      @RequestParam(required = false) Boolean expiringSoon,
      @RequestParam(required = false) Boolean available,
      @RequestParam(required = false) String lotNumber,
      @PageableDefault(size = 20, sort = "receivedDate") Pageable p) {
    access.requireInventoryRead(organizationId);
    return inventory.list(
        organizationId,
        pantryId,
        itemId,
        expired,
        expiringSoon,
        available,
        lotNumber,
        SafePageables.allow(
            p, Set.of("receivedDate", "expirationDate", "quantityRemaining", "createdAt")));
  }

  @GetMapping("/food-pantries/{pantryId}/inventory/availability")
  public List<InventoryAvailabilityResponse> availability(
      @PathVariable UUID organizationId,
      @PathVariable UUID pantryId,
      @RequestParam(required = false) UUID itemId) {
    access.requireInventoryRead(organizationId);
    return inventory.availability(organizationId, pantryId, itemId);
  }

  @GetMapping("/pantry-inventory/{lotId}")
  public InventoryLotResponse detail(@PathVariable UUID organizationId, @PathVariable UUID lotId) {
    access.requireInventoryRead(organizationId);
    return inventory.detail(organizationId, lotId);
  }

  @PostMapping("/pantry-inventory/{lotId}/adjust")
  public InventoryLotResponse adjust(
      @PathVariable UUID organizationId,
      @PathVariable UUID lotId,
      @Valid @RequestBody AdjustInventoryRequest r) {
    access.requirePantryManagement(organizationId);
    return inventory.adjust(organizationId, lotId, access.userId(), r);
  }

  @GetMapping("/pantry-inventory/{lotId}/transactions")
  public List<InventoryTransactionResponse> history(
      @PathVariable UUID organizationId, @PathVariable UUID lotId) {
    access.requireInventoryRead(organizationId);
    return inventory.history(organizationId, lotId);
  }
}
