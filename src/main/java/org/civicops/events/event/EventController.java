package org.civicops.events.event;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.civicops.events.event.dto.*;
import org.civicops.events.registration.*;
import org.civicops.events.registration.dto.*;
import org.civicops.events.security.EventAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/events")
@Tag(name = "Events")
public class EventController {
  private final EventService events;
  private final EventRegistrationService registrations;
  private final EventAccessService access;

  public EventController(EventService e, EventRegistrationService r, EventAccessService a) {
    events = e;
    registrations = r;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EventDetailResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateEventRequest r) {
    access.requireManage(organizationId);
    return events.create(organizationId, access.userId(), r);
  }

  @GetMapping
  public Page<EventSummaryResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) EventStatus status,
      @RequestParam(required = false) EventType type,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @RequestParam(required = false) UUID grantId,
      @RequestParam(required = false) UUID campaignId,
      @PageableDefault(size = 20, sort = "startDateTime") Pageable p) {
    access.requireRead(organizationId);
    return events.list(
        organizationId,
        status,
        type,
        from,
        to,
        grantId,
        campaignId,
        SafePageables.allow(p, Set.of("name", "startDateTime", "endDateTime", "createdAt")));
  }

  @GetMapping("/{eventId}")
  public EventDetailResponse detail(@PathVariable UUID organizationId, @PathVariable UUID eventId) {
    access.requireRead(organizationId);
    return events.detail(organizationId, eventId);
  }

  @PatchMapping("/{eventId}")
  public EventDetailResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID eventId,
      @Valid @RequestBody UpdateEventRequest r) {
    access.requireManage(organizationId);
    return events.update(organizationId, eventId, r);
  }

  @PostMapping("/{eventId}/publish")
  public EventDetailResponse publish(
      @PathVariable UUID organizationId, @PathVariable UUID eventId) {
    return transition(organizationId, eventId, EventStatus.PUBLISHED);
  }

  @PostMapping("/{eventId}/open-registration")
  public EventDetailResponse open(@PathVariable UUID organizationId, @PathVariable UUID eventId) {
    return transition(organizationId, eventId, EventStatus.REGISTRATION_OPEN);
  }

  @PostMapping("/{eventId}/close-registration")
  public EventDetailResponse close(@PathVariable UUID organizationId, @PathVariable UUID eventId) {
    return transition(organizationId, eventId, EventStatus.REGISTRATION_CLOSED);
  }

  @PostMapping("/{eventId}/complete")
  public EventDetailResponse complete(
      @PathVariable UUID organizationId, @PathVariable UUID eventId) {
    return transition(organizationId, eventId, EventStatus.COMPLETED);
  }

  @PostMapping("/{eventId}/cancel")
  public EventDetailResponse cancel(@PathVariable UUID organizationId, @PathVariable UUID eventId) {
    return transition(organizationId, eventId, EventStatus.CANCELLED);
  }

  private EventDetailResponse transition(UUID org, UUID id, EventStatus status) {
    access.requireManage(org);
    return events.transition(org, id, status);
  }

  @PostMapping("/{eventId}/registrations")
  @ResponseStatus(HttpStatus.CREATED)
  public RegistrationDetailResponse register(
      @PathVariable UUID organizationId,
      @PathVariable UUID eventId,
      @Valid @RequestBody CreateRegistrationRequest r) {
    access.requireManage(organizationId);
    return registrations.register(organizationId, eventId, null, r);
  }

  @PostMapping("/{eventId}/registrations/me")
  @ResponseStatus(HttpStatus.CREATED)
  public RegistrationDetailResponse me(
      @PathVariable UUID organizationId,
      @PathVariable UUID eventId,
      @Valid @RequestBody CreateRegistrationRequest r) {
    access.requireSelfService(organizationId);
    return registrations.register(organizationId, eventId, access.userId(), r);
  }

  @GetMapping("/{eventId}/registrations/me")
  public RegistrationDetailResponse mine(
      @PathVariable UUID organizationId, @PathVariable UUID eventId) {
    access.requireSelfService(organizationId);
    return registrations.mine(organizationId, eventId, access.userId());
  }

  @PostMapping("/{eventId}/registrations/me/cancel")
  public RegistrationDetailResponse cancelMine(
      @PathVariable UUID organizationId, @PathVariable UUID eventId) {
    access.requireSelfService(organizationId);
    RegistrationDetailResponse r = registrations.mine(organizationId, eventId, access.userId());
    return registrations.cancel(organizationId, r.id());
  }

  @GetMapping("/{eventId}/registrations")
  public Page<RegistrationSummaryResponse> registrations(
      @PathVariable UUID organizationId,
      @PathVariable UUID eventId,
      @RequestParam(required = false) RegistrationStatus status,
      @RequestParam(required = false) String attendeeEmail,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @PageableDefault(size = 20, sort = "registrationDate") Pageable p) {
    access.requireRegistrationRead(organizationId);
    return registrations.list(
        organizationId,
        eventId,
        status,
        attendeeEmail,
        from,
        to,
        SafePageables.allow(p, Set.of("registrationDate", "attendeeName", "createdAt")));
  }
}
