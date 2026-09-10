package org.civicops.events.registration;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import org.civicops.events.registration.dto.*;
import org.civicops.events.security.EventAccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/event-registrations")
@Tag(name = "Event Registrations")
public class EventRegistrationController {
  private final EventRegistrationService service;
  private final EventAccessService access;

  public EventRegistrationController(EventRegistrationService s, EventAccessService a) {
    service = s;
    access = a;
  }

  @GetMapping("/{registrationId}")
  public RegistrationDetailResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID registrationId) {
    access.requireRegistrationRead(organizationId);
    return service.detail(organizationId, registrationId);
  }

  @PostMapping("/{registrationId}/cancel")
  public RegistrationDetailResponse cancel(
      @PathVariable UUID organizationId, @PathVariable UUID registrationId) {
    access.requireManage(organizationId);
    return service.cancel(organizationId, registrationId);
  }

  @PostMapping("/{registrationId}/check-in")
  public RegistrationDetailResponse checkIn(
      @PathVariable UUID organizationId, @PathVariable UUID registrationId) {
    access.requireManage(organizationId);
    return service.checkIn(organizationId, registrationId);
  }

  @PostMapping("/{registrationId}/check-out")
  public RegistrationDetailResponse checkOut(
      @PathVariable UUID organizationId, @PathVariable UUID registrationId) {
    access.requireManage(organizationId);
    return service.checkOut(organizationId, registrationId);
  }
}
