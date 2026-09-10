package org.civicops.facilities.availability;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.civicops.facilities.availability.dto.*;
import org.civicops.facilities.security.FacilityAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/facility-blackouts")
@Tag(name = "Facility Blackouts")
public class FacilityBlackoutController {
  private final FacilityBlackoutService blackouts;
  private final FacilityAccessService access;

  public FacilityBlackoutController(FacilityBlackoutService b, FacilityAccessService a) {
    blackouts = b;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BlackoutResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateBlackoutRequest r) {
    access.requireFacilityManagement(organizationId);
    return blackouts.create(organizationId, access.userId(), r);
  }

  @GetMapping
  public Page<BlackoutResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) UUID facilityId,
      @RequestParam(required = false) UUID spaceId,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @PageableDefault(size = 20, sort = "startDateTime") Pageable p) {
    access.requireFacilityRead(organizationId);
    return blackouts.list(
        organizationId,
        facilityId,
        spaceId,
        from,
        to,
        SafePageables.allow(p, Set.of("startDateTime", "createdAt")));
  }

  @PostMapping("/{blackoutId}/cancel")
  public BlackoutResponse cancel(@PathVariable UUID organizationId, @PathVariable UUID blackoutId) {
    access.requireFacilityManagement(organizationId);
    return blackouts.cancel(organizationId, blackoutId);
  }
}
