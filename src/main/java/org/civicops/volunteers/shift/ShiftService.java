package org.civicops.volunteers.shift;

import java.util.UUID;
import org.civicops.shared.exception.*;
import org.civicops.volunteers.assignment.*;
import org.civicops.volunteers.opportunity.*;
import org.civicops.volunteers.shift.dto.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService {
  private final VolunteerShiftRepository repo;
  private final OpportunityService opportunities;
  private final VolunteerAssignmentRepository assignments;

  public ShiftService(
      VolunteerShiftRepository r, OpportunityService o, VolunteerAssignmentRepository assignments) {
    repo = r;
    opportunities = o;
    this.assignments = assignments;
  }

  @Transactional
  public ShiftResponse create(UUID orgId, UUID opportunityId, CreateShiftRequest r) {
    VolunteerOpportunity o = opportunities.require(orgId, opportunityId);
    if (o.getStatus() == OpportunityStatus.CANCELLED
        || o.getStatus() == OpportunityStatus.COMPLETED)
      throw new BusinessRuleException(
          "OPPORTUNITY_NOT_SCHEDULABLE",
          "Cancelled or completed opportunities cannot receive shifts");
    if (r.startAt().isBefore(o.getStartAt())
        || r.endAt().isAfter(o.getEndAt())
        || !r.endAt().isAfter(r.startAt()))
      throw new BusinessRuleException(
          "SHIFT_OUTSIDE_OPPORTUNITY", "Shift must be within the opportunity date range");
    int cap =
        o.getMaximumVolunteers() == null
            ? r.capacity()
            : Math.min(r.capacity(), o.getMaximumVolunteers());
    if (cap != r.capacity())
      throw new BusinessRuleException(
          "SHIFT_CAPACITY_EXCEEDS_OPPORTUNITY", "Shift capacity cannot exceed opportunity maximum");
    return ShiftResponse.from(
        repo.save(
            new VolunteerShift(
                o.getOrganization(), o, r.title().trim(), r.startAt(), r.endAt(), r.capacity())));
  }

  @Transactional(readOnly = true)
  public VolunteerShift require(UUID orgId, UUID id) {
    return repo.findByIdAndOrganizationId(id, orgId)
        .orElseThrow(() -> new ResourceNotFoundException("VolunteerShift", id));
  }

  @Transactional(readOnly = true)
  public Page<ShiftResponse> list(
      UUID orgId, UUID oppId, java.time.Instant from, java.time.Instant to, Pageable p) {
    if (from != null && to != null && to.isBefore(from)) {
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Filter end must not precede start");
    }
    opportunities.require(orgId, oppId);
    Specification<VolunteerShift> spec =
        (root, query, cb) ->
            cb.and(
                cb.equal(root.get("organization").get("id"), orgId),
                cb.equal(root.get("opportunity").get("id"), oppId));
    if (from != null)
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startAt"), from));
    if (to != null)
      spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("startAt"), to));
    return repo.findAll(spec, p).map(ShiftResponse::from);
  }

  @Transactional
  public ShiftResponse update(UUID orgId, UUID opportunityId, UUID shiftId, UpdateShiftRequest r) {
    VolunteerOpportunity opportunity = opportunities.require(orgId, opportunityId);
    if (opportunity.getStatus() == OpportunityStatus.CLOSED
        || opportunity.getStatus() == OpportunityStatus.CANCELLED
        || opportunity.getStatus() == OpportunityStatus.COMPLETED) {
      throw new BusinessRuleException(
          "TERMINAL_OPPORTUNITY_NOT_EDITABLE",
          "Shifts for terminal opportunities cannot be edited");
    }
    VolunteerShift shift = require(orgId, shiftId);
    if (!shift.getOpportunity().getId().equals(opportunityId))
      throw new ResourceNotFoundException("VolunteerShift", shiftId);
    java.time.Instant start = r.startAt() == null ? shift.getStartAt() : r.startAt();
    java.time.Instant end = r.endAt() == null ? shift.getEndAt() : r.endAt();
    int capacity = r.capacity() == null ? shift.getCapacity() : r.capacity();
    if (!end.isAfter(start)
        || start.isBefore(opportunity.getStartAt())
        || end.isAfter(opportunity.getEndAt())) {
      throw new BusinessRuleException(
          "SHIFT_OUTSIDE_OPPORTUNITY", "Shift must be within the opportunity date range");
    }
    long active = assignments.countByShiftIdAndStatusNot(shiftId, AssignmentStatus.CANCELLED);
    if (capacity < active)
      throw new BusinessRuleException(
          "SHIFT_CAPACITY_BELOW_ASSIGNMENTS", "Shift capacity cannot be below active assignments");
    if (opportunity.getMaximumVolunteers() != null && capacity > opportunity.getMaximumVolunteers())
      throw new BusinessRuleException(
          "SHIFT_CAPACITY_EXCEEDS_OPPORTUNITY", "Shift capacity cannot exceed opportunity maximum");
    if ((!start.equals(shift.getStartAt()) || !end.equals(shift.getEndAt()))
        && assignments.shiftUpdateCreatesOverlap(shiftId, start, end)) {
      throw new BusinessRuleException(
          "SHIFT_UPDATE_CREATES_OVERLAP", "Shift update would create an assignment conflict");
    }
    shift.update(r.title() == null ? null : r.title().trim(), r.startAt(), r.endAt(), r.capacity());
    return ShiftResponse.from(shift);
  }
}
