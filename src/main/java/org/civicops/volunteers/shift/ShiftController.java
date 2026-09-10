package org.civicops.volunteers.shift;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.civicops.volunteers.security.VolunteerAccessService;
import org.civicops.volunteers.shift.dto.*;
import org.civicops.volunteers.support.VolunteerPageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
    "/api/v1/organizations/{organizationId}/volunteer-opportunities/{opportunityId}/shifts")
@Tag(name = "Volunteer Shifts")
public class ShiftController {
  private final ShiftService service;
  private final VolunteerAccessService access;

  public ShiftController(ShiftService s, VolunteerAccessService a) {
    service = s;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ShiftResponse create(
      @PathVariable UUID organizationId,
      @PathVariable UUID opportunityId,
      @Valid @RequestBody CreateShiftRequest r) {
    access.requireOpportunityManage(organizationId);
    return service.create(organizationId, opportunityId, r);
  }

  @GetMapping
  @Operation(
      summary = "List opportunity shifts",
      description = "Paginated; filters: from, to. Sort: startAt, title.")
  public Page<ShiftResponse> list(
      @PathVariable UUID organizationId,
      @PathVariable UUID opportunityId,
      @RequestParam(required = false) java.time.Instant from,
      @RequestParam(required = false) java.time.Instant to,
      @PageableDefault(size = 20, sort = "startAt") Pageable p) {
    access.requireActivityRead(organizationId);
    return service.list(
        organizationId,
        opportunityId,
        from,
        to,
        VolunteerPageables.allow(p, java.util.Set.of("startAt", "title")));
  }

  @PatchMapping("/{shiftId}")
  @Operation(summary = "Safely update a shift, capacity, and schedule")
  public ShiftResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID opportunityId,
      @PathVariable UUID shiftId,
      @Valid @RequestBody UpdateShiftRequest request) {
    access.requireOpportunityManage(organizationId);
    return service.update(organizationId, opportunityId, shiftId, request);
  }
}
