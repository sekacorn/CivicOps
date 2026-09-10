package org.civicops.volunteers.hours;

import jakarta.validation.Valid;
import java.util.UUID;
import org.civicops.volunteers.hours.dto.*;
import org.civicops.volunteers.security.VolunteerAccessService;
import org.civicops.volunteers.support.VolunteerPageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
public class HourEntryController {
  private final HourEntryService service;
  private final VolunteerAccessService access;

  public HourEntryController(HourEntryService s, VolunteerAccessService a) {
    service = s;
    access = a;
  }

  @PostMapping("/volunteer-hours")
  @ResponseStatus(HttpStatus.CREATED)
  public HourEntryResponse submit(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateHourEntryRequest r) {
    access.requireOwnOrManage(organizationId, r.volunteerId());
    return service.submit(organizationId, r);
  }

  @GetMapping("/volunteer-hours")
  public Page<HourEntryResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) HourEntryStatus status,
      @RequestParam(required = false) UUID volunteerId,
      @RequestParam(required = false)
          @org.springframework.format.annotation.DateTimeFormat(
              iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
          java.time.LocalDate from,
      @RequestParam(required = false)
          @org.springframework.format.annotation.DateTimeFormat(
              iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
          java.time.LocalDate to,
      @PageableDefault(size = 20, sort = "serviceDate") Pageable p) {
    access.requireManage(organizationId);
    return service.list(
        organizationId,
        status,
        volunteerId,
        from,
        to,
        VolunteerPageables.allow(p, java.util.Set.of("serviceDate", "createdAt")));
  }

  @PostMapping("/volunteer-hours/{id}/approve")
  public HourEntryResponse approve(@PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireManage(organizationId);
    return service.approve(organizationId, id, access.userId());
  }

  @PostMapping("/volunteer-hours/{id}/reject")
  public HourEntryResponse reject(
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody RejectHourEntryRequest r) {
    access.requireManage(organizationId);
    return service.reject(organizationId, id, access.userId(), r.reason());
  }

  @GetMapping("/volunteers/me/hours")
  public Page<HourEntryResponse> mine(
      @PathVariable UUID organizationId,
      @PageableDefault(size = 20, sort = "serviceDate") Pageable p) {
    return service.forVolunteer(
        organizationId, access.requireOwnVolunteer(organizationId).getId(), p);
  }
}
