package org.civicops.volunteers.volunteer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.civicops.volunteers.security.VolunteerAccessService;
import org.civicops.volunteers.support.VolunteerPageables;
import org.civicops.volunteers.volunteer.dto.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/volunteers")
@Tag(name = "Volunteers")
public class VolunteerController {
  private final VolunteerService service;
  private final VolunteerAccessService access;

  public VolunteerController(VolunteerService s, VolunteerAccessService a) {
    service = s;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public VolunteerDetailResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateVolunteerRequest r) {
    access.requireManage(organizationId);
    return service.create(organizationId, r);
  }

  @GetMapping
  @Operation(
      summary = "List volunteers",
      description =
          "Paginated; filters: status, normalized email, skill. Sort: lastName, createdAt.")
  public Page<VolunteerSummaryResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) VolunteerStatus status,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String skill,
      @PageableDefault(size = 20, sort = "lastName") Pageable p) {
    access.requireRead(organizationId);
    return service.list(
        organizationId,
        status,
        email,
        skill,
        VolunteerPageables.allow(p, java.util.Set.of("lastName", "createdAt")));
  }

  @GetMapping("/{volunteerId}")
  public VolunteerDetailResponse get(
      @PathVariable UUID organizationId, @PathVariable UUID volunteerId) {
    access.requireManage(organizationId);
    return service.detail(organizationId, volunteerId);
  }

  @PatchMapping("/{volunteerId}")
  @Operation(summary = "Update a volunteer profile without changing status or linked user")
  public VolunteerDetailResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID volunteerId,
      @Valid @RequestBody UpdateVolunteerRequest request) {
    access.requireManage(organizationId);
    return service.update(organizationId, volunteerId, request);
  }

  @PostMapping("/{volunteerId}/status")
  @Operation(summary = "Apply an explicit volunteer status transition")
  public VolunteerDetailResponse changeStatus(
      @PathVariable UUID organizationId,
      @PathVariable UUID volunteerId,
      @Valid @RequestBody UpdateVolunteerStatusRequest r) {
    access.requireManage(organizationId);
    return service.changeStatus(organizationId, volunteerId, r.status());
  }

  @GetMapping("/me")
  public VolunteerDetailResponse me(@PathVariable UUID organizationId) {
    return VolunteerDetailResponse.from(access.requireOwnVolunteer(organizationId));
  }
}
