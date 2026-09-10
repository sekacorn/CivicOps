package org.civicops.facilities.reservation;

import java.time.*;
import java.util.*;
import org.civicops.core.membership.OrganizationMembershipRepository;
import org.civicops.core.user.*;
import org.civicops.events.event.*;
import org.civicops.facilities.availability.AvailabilityService;
import org.civicops.facilities.reservation.dto.*;
import org.civicops.facilities.space.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacilityReservationService {
  private final FacilityReservationRepository reservations;
  private final FacilitySpaceService spaces;
  private final AvailabilityService availability;
  private final UserService users;
  private final OrganizationMembershipRepository memberships;
  private final EventRecordRepository events;
  private final Clock clock;

  public FacilityReservationService(
      FacilityReservationRepository r,
      FacilitySpaceService s,
      AvailabilityService a,
      UserService u,
      OrganizationMembershipRepository m,
      EventRecordRepository e,
      Clock c) {
    reservations = r;
    spaces = s;
    availability = a;
    users = u;
    memberships = m;
    events = e;
    clock = c;
  }

  @Transactional
  public ReservationDetailResponse create(UUID org, UUID actor, CreateReservationRequest r) {
    FacilitySpace s = spaces.require(org, r.facilitySpaceId());
    validateTimes(r.startDateTime(), r.endDateTime());
    User requester = null;
    String name = clean(r.requesterName()), email = email(r.requesterEmail());
    UUID requesterId = r.requestedByUserId();
    if (requesterId == null && name == null) requesterId = actor;
    if (requesterId != null) requester = member(org, requesterId);
    else if (name == null)
      throw new BusinessRuleException("REQUESTER_REQUIRED", "External requester name is required");
    EventRecord event = event(org, r.eventId(), r.startDateTime(), r.endDateTime());
    if (r.expectedAttendance() != null
        && s.getCapacity() != null
        && r.expectedAttendance() > s.getCapacity())
      throw new BusinessRuleException(
          "SPACE_CAPACITY_EXCEEDED", "Expected attendance exceeds space capacity");
    availability.requireAvailable(org, s, r.startDateTime(), r.endDateTime(), new UUID(0, 0));
    return ReservationDetailResponse.from(
        reservations.save(
            new FacilityReservation(
                s,
                requester,
                name,
                email,
                event,
                r.title().trim(),
                clean(r.purpose()),
                r.startDateTime(),
                r.endDateTime(),
                r.expectedAttendance(),
                Instant.now(clock),
                clean(r.notes()))));
  }

  @Transactional
  public ReservationDetailResponse update(UUID org, UUID id, UpdateReservationRequest r) {
    FacilityReservation x = require(org, id);
    Instant start = r.startDateTime() == null ? x.getStartDateTime() : r.startDateTime(),
        end = r.endDateTime() == null ? x.getEndDateTime() : r.endDateTime();
    validateTimes(start, end);
    if (r.expectedAttendance() != null
        && x.getFacilitySpace().getCapacity() != null
        && r.expectedAttendance() > x.getFacilitySpace().getCapacity())
      throw new BusinessRuleException(
          "SPACE_CAPACITY_EXCEEDED", "Expected attendance exceeds space capacity");
    eventWindow(x.getEvent(), start, end);
    availability.requireAvailable(org, x.getFacilitySpace(), start, end, x.getId());
    x.update(
        clean(r.title()),
        clean(r.purpose()),
        r.startDateTime(),
        r.endDateTime(),
        r.expectedAttendance(),
        clean(r.notes()));
    return ReservationDetailResponse.from(x);
  }

  @Transactional
  public ReservationDetailResponse approve(UUID org, UUID id, UUID actor) {
    FacilityReservation x = require(org, id);
    FacilitySpace locked = spaces.requireLocked(org, x.getFacilitySpace().getId());
    availability.requireAvailable(org, locked, x.getStartDateTime(), x.getEndDateTime(), x.getId());
    x.approve(member(org, actor), Instant.now(clock));
    return ReservationDetailResponse.from(x);
  }

  @Transactional
  public ReservationDetailResponse reject(UUID org, UUID id, UUID actor, String reason) {
    FacilityReservation x = require(org, id);
    x.reject(member(org, actor), Instant.now(clock), reason.trim());
    return ReservationDetailResponse.from(x);
  }

  @Transactional
  public ReservationDetailResponse cancel(UUID org, UUID id, String reason) {
    FacilityReservation x = require(org, id);
    x.cancel(Instant.now(clock), clean(reason));
    return ReservationDetailResponse.from(x);
  }

  @Transactional
  public ReservationDetailResponse complete(UUID org, UUID id) {
    FacilityReservation x = require(org, id);
    x.complete(Instant.now(clock));
    return ReservationDetailResponse.from(x);
  }

  @Transactional(readOnly = true)
  public FacilityReservation require(UUID org, UUID id) {
    return reservations
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Facility reservation", id));
  }

  @Transactional(readOnly = true)
  public ReservationDetailResponse detail(UUID org, UUID id) {
    return ReservationDetailResponse.from(require(org, id));
  }

  @Transactional(readOnly = true)
  public Page<ReservationSummaryResponse> list(
      UUID org,
      ReservationStatus status,
      UUID facility,
      UUID space,
      UUID requester,
      UUID event,
      Instant from,
      Instant to,
      Pageable p) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Filter end precedes start");
    Specification<FacilityReservation> s =
        (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (facility != null)
      s = s.and((r, q, c) -> c.equal(r.get("facilitySpace").get("facility").get("id"), facility));
    if (space != null) s = s.and((r, q, c) -> c.equal(r.get("facilitySpace").get("id"), space));
    if (requester != null)
      s = s.and((r, q, c) -> c.equal(r.get("requestedByUser").get("id"), requester));
    if (event != null) s = s.and((r, q, c) -> c.equal(r.get("event").get("id"), event));
    if (from != null) s = s.and((r, q, c) -> c.greaterThan(r.get("endDateTime"), from));
    if (to != null) s = s.and((r, q, c) -> c.lessThan(r.get("startDateTime"), to));
    return reservations.findAll(s, p).map(ReservationSummaryResponse::from);
  }

  @Transactional(readOnly = true)
  public Page<ReservationSummaryResponse> mine(UUID org, UUID user, Pageable p) {
    Specification<FacilityReservation> s =
        (r, q, c) ->
            c.and(
                c.equal(r.get("organization").get("id"), org),
                c.equal(r.get("requestedByUser").get("id"), user));
    return reservations.findAll(s, p).map(ReservationSummaryResponse::from);
  }

  private User member(UUID org, UUID id) {
    User u = users.requireEntity(id);
    if (!u.isActive() || memberships.findByOrganizationIdAndUserIdAndActiveTrue(org, id).isEmpty())
      throw new BusinessRuleException(
          "INVALID_RESERVATION_REQUESTER",
          "Reservation user must be an active organization member");
    return u;
  }

  private EventRecord event(UUID org, UUID id, Instant start, Instant end) {
    if (id == null) return null;
    EventRecord e =
        events
            .findByIdAndOrganizationId(id, org)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "INVALID_RESERVATION_EVENT",
                        "Event must belong to the reservation organization"));
    eventWindow(e, start, end);
    return e;
  }

  private static void eventWindow(EventRecord e, Instant start, Instant end) {
    if (e != null && (e.getStartDateTime().isBefore(start) || e.getEndDateTime().isAfter(end)))
      throw new BusinessRuleException(
          "EVENT_OUTSIDE_RESERVATION", "Reservation must contain the linked Event window");
  }

  private static void validateTimes(Instant start, Instant end) {
    if (start == null || end == null || !end.isAfter(start))
      throw new BusinessRuleException(
          "INVALID_RESERVATION_TIME", "Reservation end must be after start");
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  private static String email(String s) {
    return clean(s) == null ? null : UserService.normalizeEmail(s);
  }
}
