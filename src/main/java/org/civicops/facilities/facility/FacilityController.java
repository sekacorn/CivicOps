package org.civicops.facilities.facility;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.facilities.availability.*;
import org.civicops.facilities.availability.dto.*;
import org.civicops.facilities.facility.dto.*;
import org.civicops.facilities.security.FacilityAccessService;
import org.civicops.facilities.space.*;
import org.civicops.facilities.space.dto.*;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/facilities")
@Tag(name = "Facilities")
public class FacilityController {
  private final FacilityService facilities;
  private final FacilitySpaceService spaces;
  private final OperatingHoursService hours;
  private final FacilityAccessService access;

  public FacilityController(
      FacilityService f, FacilitySpaceService s, OperatingHoursService h, FacilityAccessService a) {
    facilities = f;
    spaces = s;
    hours = h;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FacilityResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateFacilityRequest r) {
    access.requireFacilityManagement(organizationId);
    return facilities.create(organizationId, r);
  }

  @GetMapping
  public Page<FacilityResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) FacilityType type,
      @RequestParam(required = false) String city,
      @PageableDefault(size = 20, sort = "name") Pageable p) {
    access.requireFacilityRead(organizationId);
    return facilities.list(
        organizationId, active, type, city, SafePageables.allow(p, Set.of("name", "createdAt")));
  }

  @GetMapping("/{facilityId}")
  public FacilityResponse detail(@PathVariable UUID organizationId, @PathVariable UUID facilityId) {
    access.requireFacilityRead(organizationId);
    return facilities.detail(organizationId, facilityId);
  }

  @PatchMapping("/{facilityId}")
  public FacilityResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID facilityId,
      @Valid @RequestBody UpdateFacilityRequest r) {
    access.requireFacilityManagement(organizationId);
    return facilities.update(organizationId, facilityId, r);
  }

  @PostMapping("/{facilityId}/spaces")
  @ResponseStatus(HttpStatus.CREATED)
  public FacilitySpaceResponse space(
      @PathVariable UUID organizationId,
      @PathVariable UUID facilityId,
      @Valid @RequestBody CreateFacilitySpaceRequest r) {
    access.requireFacilityManagement(organizationId);
    return spaces.create(organizationId, facilityId, r);
  }

  @GetMapping("/{facilityId}/spaces")
  public Page<FacilitySpaceResponse> spaces(
      @PathVariable UUID organizationId,
      @PathVariable UUID facilityId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) Boolean reservable,
      @RequestParam(required = false) Integer minimumCapacity,
      @PageableDefault(size = 20, sort = "name") Pageable p) {
    access.requireFacilityRead(organizationId);
    facilities.require(organizationId, facilityId);
    return spaces.list(
        organizationId,
        facilityId,
        active,
        reservable,
        minimumCapacity,
        SafePageables.allow(p, Set.of("name", "capacity", "createdAt")));
  }

  @PutMapping("/{facilityId}/operating-hours")
  public List<OperatingHoursResponse> hours(
      @PathVariable UUID organizationId,
      @PathVariable UUID facilityId,
      @Valid @RequestBody List<@Valid OperatingHoursRequest> r) {
    access.requireFacilityManagement(organizationId);
    return hours.replace(organizationId, facilityId, r);
  }

  @GetMapping("/{facilityId}/operating-hours")
  public List<OperatingHoursResponse> hours(
      @PathVariable UUID organizationId, @PathVariable UUID facilityId) {
    access.requireFacilityRead(organizationId);
    return hours.list(organizationId, facilityId);
  }
}
