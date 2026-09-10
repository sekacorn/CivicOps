package org.civicops.events.registration;

import java.time.*;
import java.util.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.*;
import org.civicops.events.event.*;
import org.civicops.events.registration.dto.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventRegistrationService {
  private static final List<RegistrationStatus> ACTIVE =
      List.of(
          RegistrationStatus.REGISTERED,
          RegistrationStatus.WAITLISTED,
          RegistrationStatus.ATTENDED);
  private final EventRegistrationRepository registrations;
  private final EventService events;
  private final OrganizationService orgs;
  private final UserRepository users;
  private final Clock clock;

  public EventRegistrationService(
      EventRegistrationRepository r,
      EventService e,
      OrganizationService o,
      UserRepository u,
      Clock c) {
    registrations = r;
    events = e;
    orgs = o;
    users = u;
    clock = c;
  }

  @Transactional
  public RegistrationDetailResponse register(
      UUID org, UUID eventId, UUID registeredUserId, CreateRegistrationRequest r) {
    EventRecord event = events.requireLocked(org, eventId);
    Instant now = clock.instant();
    if (!event.acceptsRegistrations(now))
      throw new BusinessRuleException(
          "EVENT_NOT_ACCEPTING_REGISTRATIONS", "Event is not accepting registrations");
    String email = normalize(r.attendeeEmail());
    User user =
        registeredUserId == null
            ? null
            : users
                .findById(registeredUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", registeredUserId));
    String name = clean(r.attendeeName());
    if (user != null) {
      if (registrations.existsByEventIdAndRegisteredUserIdAndStatusIn(
          eventId, registeredUserId, ACTIVE))
        throw new ConflictException(
            "DUPLICATE_REGISTRATION", "User already has an active registration");
      if (name == null) name = user.getFirstName() + " " + user.getLastName();
      if (email == null) email = user.getEmail();
    } else if (name == null || email == null)
      throw new BusinessRuleException(
          "EXTERNAL_CONTACT_REQUIRED", "External attendees require name and email");
    if (email != null
        && registrations.existsByEventIdAndAttendeeEmailIgnoreCaseAndStatusIn(
            eventId, email, ACTIVE))
      throw new ConflictException(
          "DUPLICATE_REGISTRATION", "Email already has an active registration");
    long seated = registrations.countSeated(eventId);
    RegistrationStatus status =
        event.getCapacity() == null || seated < event.getCapacity()
            ? RegistrationStatus.REGISTERED
            : (event.isWaitlistEnabled() ? RegistrationStatus.WAITLISTED : null);
    if (status == null)
      throw new BusinessRuleException("EVENT_AT_CAPACITY", "Event capacity has been reached");
    return RegistrationDetailResponse.from(
        registrations.save(
            new EventRegistration(
                orgs.requireEntity(org),
                event,
                user,
                name,
                email,
                clean(r.attendeePhone()),
                status,
                now)));
  }

  @Transactional(readOnly = true)
  public RegistrationDetailResponse detail(UUID org, UUID id) {
    return RegistrationDetailResponse.from(require(org, id));
  }

  @Transactional(readOnly = true)
  public EventRegistration require(UUID org, UUID id) {
    return registrations
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("EventRegistration", id));
  }

  @Transactional(readOnly = true)
  public Page<RegistrationSummaryResponse> list(
      UUID org,
      UUID eventId,
      RegistrationStatus status,
      String email,
      Instant from,
      Instant to,
      Pageable p) {
    events.require(org, eventId);
    Specification<EventRegistration> s = (r, q, c) -> c.equal(r.get("event").get("id"), eventId);
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (email != null)
      s = s.and((r, q, c) -> c.equal(c.lower(r.get("attendeeEmail")), normalize(email)));
    if (from != null)
      s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("registrationDate"), from));
    if (to != null) s = s.and((r, q, c) -> c.lessThanOrEqualTo(r.get("registrationDate"), to));
    return registrations.findAll(s, p).map(RegistrationSummaryResponse::from);
  }

  @Transactional
  public RegistrationDetailResponse cancel(UUID org, UUID id) {
    EventRegistration r = require(org, id);
    EventRecord event = events.requireLocked(org, r.getEvent().getId());
    RegistrationStatus before = r.getStatus();
    r.cancel();
    if (before == RegistrationStatus.REGISTERED) promote(event);
    return RegistrationDetailResponse.from(r);
  }

  @Transactional
  public RegistrationDetailResponse checkIn(UUID org, UUID id) {
    EventRegistration r = require(org, id);
    r.checkIn(clock.instant());
    return RegistrationDetailResponse.from(r);
  }

  @Transactional
  public RegistrationDetailResponse checkOut(UUID org, UUID id) {
    EventRegistration r = require(org, id);
    r.checkOut(clock.instant());
    return RegistrationDetailResponse.from(r);
  }

  @Transactional(readOnly = true)
  public RegistrationDetailResponse mine(UUID org, UUID eventId, UUID user) {
    return RegistrationDetailResponse.from(
        registrations
            .findAll(
                (root, q, cb) ->
                    cb.and(
                        cb.equal(root.get("event").get("id"), eventId),
                        cb.equal(root.get("registeredUser").get("id"), user)),
                PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("EventRegistration", eventId)));
  }

  private void promote(EventRecord e) {
    if (e.getCapacity() == null || registrations.countSeated(e.getId()) >= e.getCapacity()) return;
    registrations.waitlisted(e.getId()).stream().findFirst().ifPresent(EventRegistration::promote);
  }

  private static String normalize(String s) {
    return s == null ? null : s.trim().toLowerCase(java.util.Locale.ROOT);
  }

  private static String clean(String s) {
    return s == null ? null : s.trim();
  }
}
