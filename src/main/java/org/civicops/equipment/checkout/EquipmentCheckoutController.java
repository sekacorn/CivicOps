package org.civicops.equipment.checkout;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.civicops.equipment.checkout.dto.*;
import org.civicops.equipment.security.EquipmentAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/equipment-checkouts")
@Tag(name = "Equipment Checkouts")
public class EquipmentCheckoutController {
  private final EquipmentCheckoutService checkouts;
  private final EquipmentAccessService access;

  public EquipmentCheckoutController(EquipmentCheckoutService c, EquipmentAccessService a) {
    checkouts = c;
    access = a;
  }

  @GetMapping
  public Page<EquipmentCheckoutSummaryResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) CheckoutStatus status,
      @RequestParam(required = false) UUID assetId,
      @RequestParam(required = false) UUID borrowerUserId,
      @RequestParam(required = false) Boolean overdue,
      @RequestParam(required = false) Instant checkedOutFrom,
      @RequestParam(required = false) Instant checkedOutTo,
      @RequestParam(required = false) Instant dueFrom,
      @RequestParam(required = false) Instant dueTo,
      @PageableDefault(size = 20, sort = "checkedOutAt", direction = Sort.Direction.DESC)
          Pageable p) {
    access.requireCheckoutManagement(organizationId);
    return checkouts.list(
        organizationId,
        assetId,
        status,
        borrowerUserId,
        overdue,
        checkedOutFrom,
        checkedOutTo,
        dueFrom,
        dueTo,
        SafePageables.allow(p, Set.of("checkedOutAt", "dueAt", "checkedInAt", "createdAt")));
  }

  @GetMapping("/{checkoutId}")
  public EquipmentCheckoutDetailResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID checkoutId) {
    access.requireCheckoutManagement(organizationId);
    return checkouts.detail(organizationId, checkoutId);
  }

  @PostMapping("/{checkoutId}/check-in")
  public EquipmentCheckoutDetailResponse checkIn(
      @PathVariable UUID organizationId,
      @PathVariable UUID checkoutId,
      @Valid @RequestBody CheckInEquipmentRequest r) {
    access.requireCheckoutManagement(organizationId);
    return checkouts.checkIn(organizationId, checkoutId, access.userId(), r);
  }

  @PostMapping("/{checkoutId}/mark-lost")
  public EquipmentCheckoutDetailResponse lost(
      @PathVariable UUID organizationId, @PathVariable UUID checkoutId) {
    access.requireCheckoutManagement(organizationId);
    return checkouts.markLost(organizationId, checkoutId);
  }
}
