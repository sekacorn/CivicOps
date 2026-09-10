package org.civicops.facilities.space;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.civicops.facilities.availability.AvailabilityService;
import org.civicops.facilities.availability.dto.AvailabilityResponse;
import org.civicops.facilities.security.FacilityAccessService;
import org.civicops.facilities.space.dto.*;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/facility-spaces")
@Tag(name = "Facility Spaces and Availability")
public class FacilitySpaceController {
  private final FacilitySpaceService spaces;
  private final AvailabilityService availability;
  private final FacilityAccessService access;

  public FacilitySpaceController(
      FacilitySpaceService s, AvailabilityService a, FacilityAccessService x) {
    spaces = s;
    availability = a;
    access = x;
  }

  @GetMapping
  public Page<FacilitySpaceResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) UUID facilityId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) Boolean reservable,
      @RequestParam(required = false) Integer minimumCapacity,
      @PageableDefault(size = 20, sort = "name") Pageable p) {
    access.requireFacilityRead(organizationId);
    return spaces.list(
        organizationId,
        facilityId,
        active,
        reservable,
        minimumCapacity,
        SafePageables.allow(p, Set.of("name", "capacity", "createdAt")));
  }

  @GetMapping("/{spaceId}")
  public FacilitySpaceResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID spaceId) {
    access.requireFacilityRead(organizationId);
    return spaces.detail(organizationId, spaceId);
  }

  @PatchMapping("/{spaceId}")
  public FacilitySpaceResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID spaceId,
      @Valid @RequestBody UpdateFacilitySpaceRequest r) {
    access.requireFacilityManagement(organizationId);
    return spaces.update(organizationId, spaceId, r);
  }

  @GetMapping("/{spaceId}/availability")
  public AvailabilityResponse available(
      @PathVariable UUID organizationId,
      @PathVariable UUID spaceId,
      @RequestParam Instant start,
      @RequestParam Instant end) {
    access.requireFacilityRead(organizationId);
    return availability.check(organizationId, spaceId, start, end);
  }
}
