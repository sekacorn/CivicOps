package org.civicops.volunteers.assignment;

import jakarta.validation.Valid;
import java.util.UUID;
import org.civicops.volunteers.assignment.dto.*;
import org.civicops.volunteers.security.VolunteerAccessService;
import org.civicops.volunteers.support.VolunteerPageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
public class AssignmentController {
  private final AssignmentService service;
  private final VolunteerAccessService access;

  public AssignmentController(AssignmentService s, VolunteerAccessService a) {
    service = s;
    access = a;
  }

  @PostMapping("/volunteer-shifts/{shiftId}/assignments")
  @ResponseStatus(HttpStatus.CREATED)
  public AssignmentResponse register(
      @PathVariable UUID organizationId,
      @PathVariable UUID shiftId,
      @Valid @RequestBody CreateAssignmentRequest r) {
    access.requireOwnOrManage(organizationId, r.volunteerId());
    return service.register(organizationId, shiftId, r.volunteerId());
  }

  @GetMapping("/volunteer-shifts/{shiftId}/assignments")
  public Page<AssignmentResponse> forShift(
      @PathVariable UUID organizationId,
      @PathVariable UUID shiftId,
      @RequestParam(required = false) UUID volunteerId,
      @RequestParam(required = false) AssignmentStatus status,
      @PageableDefault(size = 20) Pageable p) {
    access.requireRead(organizationId);
    return service.forShift(
        organizationId,
        shiftId,
        volunteerId,
        status,
        VolunteerPageables.allow(p, java.util.Set.of("createdAt", "status")));
  }

  @PostMapping("/volunteer-assignments/{id}/cancel")
  public AssignmentResponse cancel(@PathVariable UUID organizationId, @PathVariable UUID id) {
    VolunteerAssignment a = service.require(organizationId, id);
    access.requireOwnOrManage(organizationId, a.getVolunteer().getId());
    return service.cancel(organizationId, id);
  }

  @PostMapping("/volunteer-assignments/{id}/check-in")
  public AssignmentResponse checkIn(@PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireManage(organizationId);
    return service.checkIn(organizationId, id);
  }

  @PostMapping("/volunteer-assignments/{id}/check-out")
  public AssignmentResponse checkOut(@PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireManage(organizationId);
    return service.checkOut(organizationId, id);
  }

  @GetMapping("/volunteers/me/assignments")
  public Page<AssignmentResponse> mine(
      @PathVariable UUID organizationId,
      @PageableDefault(size = 20, sort = "createdAt") Pageable p) {
    return service.forVolunteer(
        organizationId, access.requireOwnVolunteer(organizationId).getId(), p);
  }
}
