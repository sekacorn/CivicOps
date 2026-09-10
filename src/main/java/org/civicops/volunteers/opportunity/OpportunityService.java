package org.civicops.volunteers.opportunity;

import java.util.UUID;
import org.civicops.core.organization.OrganizationService;
import org.civicops.events.event.EventRecordRepository;
import org.civicops.shared.exception.*;
import org.civicops.volunteers.opportunity.dto.*;
import org.civicops.volunteers.shift.VolunteerShiftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityService {
  private final VolunteerOpportunityRepository repo;
  private final OrganizationService orgs;
  private final VolunteerShiftRepository shifts;
  private final EventRecordRepository events;

  public OpportunityService(
      VolunteerOpportunityRepository r, OrganizationService o, VolunteerShiftRepository shifts) {
    this(r, o, shifts, null);
  }

  @Autowired
  public OpportunityService(
      VolunteerOpportunityRepository r,
      OrganizationService o,
      VolunteerShiftRepository shifts,
      EventRecordRepository events) {
    repo = r;
    orgs = o;
    this.shifts = shifts;
    this.events = events;
  }

  @Transactional
  public OpportunityResponse create(UUID orgId, CreateOpportunityRequest r) {
    if (!r.endAt().isAfter(r.startAt()))
      throw new BusinessRuleException(
          "INVALID_DATE_RANGE", "Opportunity end must be after its start");
    if (r.minimumVolunteers() != null
        && r.maximumVolunteers() != null
        && r.minimumVolunteers() > r.maximumVolunteers())
      throw new BusinessRuleException(
          "INVALID_VOLUNTEER_RANGE", "Minimum volunteers cannot exceed maximum volunteers");
    VolunteerOpportunity opportunity =
        repo.save(
            new VolunteerOpportunity(
                orgs.requireEntity(orgId),
                r.title().trim(),
                r.description(),
                r.location(),
                r.startAt(),
                r.endAt(),
                r.minimumVolunteers(),
                r.maximumVolunteers()));
    linkEvent(orgId, opportunity, r.eventId());
    return OpportunityResponse.from(opportunity);
  }

  @Transactional(readOnly = true)
  public VolunteerOpportunity require(UUID orgId, UUID id) {
    return repo.findByIdAndOrganizationId(id, orgId)
        .orElseThrow(() -> new ResourceNotFoundException("VolunteerOpportunity", id));
  }

  @Transactional(readOnly = true)
  public OpportunityResponse get(UUID orgId, UUID id) {
    return OpportunityResponse.from(require(orgId, id));
  }

  @Transactional(readOnly = true)
  public Page<OpportunityResponse> list(
      UUID orgId,
      OpportunityStatus status,
      java.time.Instant from,
      java.time.Instant to,
      Pageable p) {
    if (from != null && to != null && to.isBefore(from)) {
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Filter end must not precede start");
    }
    Specification<VolunteerOpportunity> spec =
        (root, query, cb) -> cb.equal(root.get("organization").get("id"), orgId);
    if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    if (from != null)
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startAt"), from));
    if (to != null)
      spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("startAt"), to));
    return repo.findAll(spec, p).map(OpportunityResponse::from);
  }

  @Transactional
  public OpportunityResponse update(UUID orgId, UUID id, UpdateOpportunityRequest r) {
    VolunteerOpportunity opportunity = require(orgId, id);
    java.time.Instant start = r.startAt() == null ? opportunity.getStartAt() : r.startAt();
    java.time.Instant end = r.endAt() == null ? opportunity.getEndAt() : r.endAt();
    Integer minimum =
        r.minimumVolunteers() == null ? opportunity.getMinimumVolunteers() : r.minimumVolunteers();
    Integer maximum =
        r.maximumVolunteers() == null ? opportunity.getMaximumVolunteers() : r.maximumVolunteers();
    validate(start, end, minimum, maximum);
    if (shifts.countOutsideWindow(id, start, end) > 0) {
      throw new BusinessRuleException(
          "OPPORTUNITY_UPDATE_INVALIDATES_SHIFTS",
          "Opportunity window must contain all existing shifts");
    }
    opportunity.update(
        trim(r.title()),
        clean(r.description()),
        clean(r.location()),
        r.startAt(),
        r.endAt(),
        r.minimumVolunteers(),
        r.maximumVolunteers());
    if (r.eventId() != null) linkEvent(orgId, opportunity, r.eventId());
    return OpportunityResponse.from(opportunity);
  }

  private void linkEvent(UUID orgId, VolunteerOpportunity opportunity, UUID eventId) {
    if (eventId == null) return;
    if (events == null)
      throw new BusinessRuleException("EVENT_LINKING_UNAVAILABLE", "Event linking is unavailable");
    opportunity.linkEvent(
        events
            .findByIdAndOrganizationId(eventId, orgId)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "INVALID_EVENT_REFERENCE", "Event must belong to the organization")));
  }

  private static void validate(
      java.time.Instant start, java.time.Instant end, Integer minimum, Integer maximum) {
    if (!end.isAfter(start))
      throw new BusinessRuleException(
          "INVALID_DATE_RANGE", "Opportunity end must be after its start");
    if (minimum != null && maximum != null && minimum > maximum)
      throw new BusinessRuleException(
          "INVALID_VOLUNTEER_RANGE", "Minimum volunteers cannot exceed maximum volunteers");
  }

  private static String trim(String value) {
    return value == null ? null : value.trim();
  }

  private static String clean(String value) {
    return value == null ? null : value.trim();
  }

  @Transactional
  public OpportunityResponse transition(UUID orgId, UUID id, String action) {
    VolunteerOpportunity o = require(orgId, id);
    switch (action) {
      case "open" -> o.open();
      case "close" -> o.close();
      case "cancel" -> o.cancel();
      case "complete" -> o.complete();
      default -> throw new IllegalArgumentException(action);
    }
    return OpportunityResponse.from(o);
  }
}
