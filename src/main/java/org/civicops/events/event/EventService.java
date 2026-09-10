package org.civicops.events.event;

import java.time.*;
import java.util.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.UserService;
import org.civicops.donations.campaign.*;
import org.civicops.events.event.dto.*;
import org.civicops.events.registration.EventRegistrationRepository;
import org.civicops.grants.grant.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {
  private final EventRecordRepository events;
  private final EventRegistrationRepository registrations;
  private final OrganizationService organizations;
  private final UserService users;
  private final GrantRepository grants;
  private final DonationCampaignRepository campaigns;
  private final Clock clock;

  public EventService(
      EventRecordRepository e,
      EventRegistrationRepository r,
      OrganizationService o,
      UserService u,
      GrantRepository g,
      DonationCampaignRepository c,
      Clock clock) {
    events = e;
    registrations = r;
    organizations = o;
    users = u;
    grants = g;
    campaigns = c;
    this.clock = clock;
  }

  @Transactional
  public EventDetailResponse create(UUID org, UUID user, CreateEventRequest r) {
    validate(r.startDateTime(), r.endDateTime(), r.capacity(), r.registrationDeadline());
    return EventDetailResponse.from(
        events.save(
            new EventRecord(
                organizations.requireEntity(org),
                users.requireEntity(user),
                clean(r.name()),
                clean(r.description()),
                r.eventType(),
                clean(r.location()),
                r.startDateTime(),
                r.endDateTime(),
                r.capacity(),
                r.registrationRequired(),
                r.registrationDeadline(),
                r.waitlistEnabled(),
                grant(org, r.linkedGrantId()),
                campaign(org, r.linkedDonationCampaignId()))));
  }

  @Transactional(readOnly = true)
  public EventRecord require(UUID org, UUID id) {
    return events
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Event", id));
  }

  @Transactional(readOnly = true)
  public EventDetailResponse detail(UUID org, UUID id) {
    return EventDetailResponse.from(require(org, id));
  }

  @Transactional(readOnly = true)
  public Page<EventSummaryResponse> list(
      UUID org,
      EventStatus status,
      EventType type,
      Instant from,
      Instant to,
      UUID grant,
      UUID campaign,
      Pageable page) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Filter end must not precede start");
    Specification<EventRecord> s =
        (root, q, cb) -> cb.equal(root.get("organization").get("id"), org);
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (type != null) s = s.and((r, q, c) -> c.equal(r.get("eventType"), type));
    if (from != null) s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("startDateTime"), from));
    if (to != null) s = s.and((r, q, c) -> c.lessThanOrEqualTo(r.get("startDateTime"), to));
    if (grant != null) s = s.and((r, q, c) -> c.equal(r.get("linkedGrant").get("id"), grant));
    if (campaign != null)
      s = s.and((r, q, c) -> c.equal(r.get("linkedDonationCampaign").get("id"), campaign));
    return events.findAll(s, page).map(EventSummaryResponse::from);
  }

  @Transactional
  public EventDetailResponse update(UUID org, UUID id, UpdateEventRequest r) {
    EventRecord e = require(org, id);
    Instant start = r.startDateTime() == null ? e.getStartDateTime() : r.startDateTime(),
        end = r.endDateTime() == null ? e.getEndDateTime() : r.endDateTime(),
        deadline =
            r.registrationDeadline() == null
                ? e.getRegistrationDeadline()
                : r.registrationDeadline();
    Integer capacity = r.capacity() == null ? e.getCapacity() : r.capacity();
    validate(start, end, capacity, deadline);
    if (r.capacity() != null && registrations.countSeated(id) > r.capacity())
      throw new BusinessRuleException(
          "CAPACITY_BELOW_ACTIVE_REGISTRATIONS",
          "Capacity cannot be lower than active registered attendees");
    e.update(
        clean(r.name()),
        clean(r.description()),
        r.eventType(),
        clean(r.location()),
        r.startDateTime(),
        r.endDateTime(),
        r.capacity(),
        r.registrationRequired(),
        r.registrationDeadline(),
        r.waitlistEnabled(),
        r.linkedGrantId() == null ? e.getLinkedGrant() : grant(org, r.linkedGrantId()),
        r.linkedDonationCampaignId() == null
            ? e.getLinkedDonationCampaign()
            : campaign(org, r.linkedDonationCampaignId()));
    return EventDetailResponse.from(e);
  }

  @Transactional
  public EventDetailResponse transition(UUID org, UUID id, EventStatus target) {
    EventRecord e = require(org, id);
    e.transition(target);
    return EventDetailResponse.from(e);
  }

  @Transactional(readOnly = true)
  public EventRecord requireLocked(UUID org, UUID id) {
    return events
        .findLockedByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Event", id));
  }

  private Grant grant(UUID org, UUID id) {
    return id == null
        ? null
        : grants
            .findByIdAndOrganizationId(id, org)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "INVALID_EVENT_GRANT", "Linked grant must belong to the organization"));
  }

  private DonationCampaign campaign(UUID org, UUID id) {
    return id == null
        ? null
        : campaigns
            .findByIdAndOrganizationId(id, org)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "INVALID_EVENT_CAMPAIGN",
                        "Linked donation campaign must belong to the organization"));
  }

  private static void validate(Instant start, Instant end, Integer capacity, Instant deadline) {
    if (!end.isAfter(start))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Event end must be after start");
    if (capacity != null && capacity < 1)
      throw new BusinessRuleException("INVALID_CAPACITY", "Capacity must be positive");
    if (deadline != null && deadline.isAfter(start))
      throw new BusinessRuleException(
          "INVALID_REGISTRATION_DEADLINE", "Registration deadline cannot be after event start");
  }

  private static String clean(String s) {
    return s == null ? null : s.trim();
  }
}
