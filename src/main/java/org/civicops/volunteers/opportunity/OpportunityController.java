package org.civicops.volunteers.opportunity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.civicops.volunteers.opportunity.dto.*;
import org.civicops.volunteers.security.VolunteerAccessService;
import org.civicops.volunteers.support.VolunteerPageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/volunteer-opportunities")
@Tag(name = "Volunteer Opportunities")
public class OpportunityController {
  private final OpportunityService service;
  private final VolunteerAccessService access;

  public OpportunityController(OpportunityService s, VolunteerAccessService a) {
    service = s;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OpportunityResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateOpportunityRequest r) {
    access.requireOpportunityManage(organizationId);
    return service.create(organizationId, r);
  }

  @GetMapping
  @Operation(
      summary = "List opportunities",
      description = "Paginated; filters: status, from, to. Sort: startAt, title.")
  public Page<OpportunityResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) OpportunityStatus status,
      @RequestParam(required = false) java.time.Instant from,
      @RequestParam(required = false) java.time.Instant to,
      @PageableDefault(size = 20, sort = "startAt") Pageable p) {
    access.requireActivityRead(organizationId);
    return service.list(
        organizationId,
        status,
        from,
        to,
        VolunteerPageables.allow(p, java.util.Set.of("startAt", "title")));
  }

  @GetMapping("/{id}")
  public OpportunityResponse get(@PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireActivityRead(organizationId);
    return service.get(organizationId, id);
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Safely update a non-terminal opportunity")
  public OpportunityResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateOpportunityRequest request) {
    access.requireOpportunityManage(organizationId);
    return service.update(organizationId, id, request);
  }

  @PostMapping("/{id}/open")
  public OpportunityResponse open(@PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireOpportunityManage(organizationId);
    return service.transition(organizationId, id, "open");
  }

  @PostMapping("/{id}/close")
  public OpportunityResponse close(@PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireOpportunityManage(organizationId);
    return service.transition(organizationId, id, "close");
  }

  @PostMapping("/{id}/cancel")
  public OpportunityResponse cancel(@PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireOpportunityManage(organizationId);
    return service.transition(organizationId, id, "cancel");
  }

  @PostMapping("/{id}/complete")
  public OpportunityResponse complete(@PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireOpportunityManage(organizationId);
    return service.transition(organizationId, id, "complete");
  }
}
