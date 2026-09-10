package org.civicops.volunteers.assignment;

import java.time.*;
import java.util.UUID;
import org.civicops.shared.exception.*;
import org.civicops.volunteers.assignment.dto.*;
import org.civicops.volunteers.opportunity.OpportunityStatus;
import org.civicops.volunteers.shift.*;
import org.civicops.volunteers.volunteer.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {
  private final VolunteerAssignmentRepository assignments;
  private final VolunteerShiftRepository shifts;
  private final VolunteerRepository volunteers;
  private final Clock clock;

  public AssignmentService(
      VolunteerAssignmentRepository a, VolunteerShiftRepository s, VolunteerRepository v, Clock c) {
    assignments = a;
    shifts = s;
    volunteers = v;
    clock = c;
  }

  @Transactional
  public AssignmentResponse register(UUID orgId, UUID shiftId, UUID volunteerId) {
    VolunteerShift shift =
        shifts
            .findForRegistration(shiftId, orgId)
            .orElseThrow(() -> new ResourceNotFoundException("VolunteerShift", shiftId));
    Volunteer volunteer =
        volunteers
            .findByIdAndOrganizationId(volunteerId, orgId)
            .orElseThrow(() -> new ResourceNotFoundException("Volunteer", volunteerId));
    if (volunteer.getStatus() != VolunteerStatus.ACTIVE)
      throw new BusinessRuleException(
          "VOLUNTEER_NOT_ACTIVE", "Only active volunteers can register");
    OpportunityStatus status = shift.getOpportunity().getStatus();
    if (status != OpportunityStatus.OPEN)
      throw new BusinessRuleException(
          "OPPORTUNITY_NOT_OPEN", "Assignments require an open opportunity");
    if (!shift.getStartAt().isAfter(clock.instant()))
      throw new BusinessRuleException(
          "SHIFT_IN_PAST", "Past or started shifts cannot accept registrations");
    if (assignments.existsByVolunteerIdAndShiftIdAndStatusNot(
        volunteerId, shiftId, AssignmentStatus.CANCELLED))
      throw new ConflictException(
          "DUPLICATE_ASSIGNMENT", "Volunteer is already assigned to this shift");
    if (assignments.hasOverlap(volunteerId, shift.getStartAt(), shift.getEndAt()))
      throw new BusinessRuleException(
          "OVERLAPPING_ASSIGNMENT", "Volunteer already has an overlapping assignment");
    if (assignments.countByShiftIdAndStatusNot(shiftId, AssignmentStatus.CANCELLED)
        >= shift.getCapacity())
      throw new ConflictException("SHIFT_FULL", "Shift capacity has been reached");
    try {
      return AssignmentResponse.from(
          assignments.saveAndFlush(
              new VolunteerAssignment(shift.getOrganization(), volunteer, shift)));
    } catch (DataIntegrityViolationException exception) {
      throw new ConflictException(
          "ASSIGNMENT_CONFLICT",
          "The volunteer could not be registered because the slot or registration was claimed"
              + " concurrently");
    }
  }

  @Transactional(readOnly = true)
  public VolunteerAssignment require(UUID orgId, UUID id) {
    return assignments
        .findByIdAndOrganizationId(id, orgId)
        .orElseThrow(() -> new ResourceNotFoundException("VolunteerAssignment", id));
  }

  @Transactional
  public AssignmentResponse cancel(UUID orgId, UUID id) {
    VolunteerAssignment a = require(orgId, id);
    a.cancel();
    return AssignmentResponse.from(a);
  }

  @Transactional
  public AssignmentResponse checkIn(UUID orgId, UUID id) {
    VolunteerAssignment a = require(orgId, id);
    a.checkIn(clock.instant());
    return AssignmentResponse.from(a);
  }

  @Transactional
  public AssignmentResponse checkOut(UUID orgId, UUID id) {
    VolunteerAssignment a = require(orgId, id);
    a.checkOut(clock.instant());
    return AssignmentResponse.from(a);
  }

  @Transactional(readOnly = true)
  public Page<AssignmentResponse> forShift(
      UUID orgId, UUID shiftId, UUID volunteerId, AssignmentStatus status, Pageable p) {
    Specification<VolunteerAssignment> spec =
        (root, query, cb) ->
            cb.and(
                cb.equal(root.get("organization").get("id"), orgId),
                cb.equal(root.get("shift").get("id"), shiftId));
    if (volunteerId != null)
      spec = spec.and((root, query, cb) -> cb.equal(root.get("volunteer").get("id"), volunteerId));
    if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    return assignments.findAll(spec, p).map(AssignmentResponse::from);
  }

  @Transactional(readOnly = true)
  public Page<AssignmentResponse> forVolunteer(UUID orgId, UUID volunteerId, Pageable p) {
    return assignments
        .findByOrganizationIdAndVolunteerId(orgId, volunteerId, p)
        .map(AssignmentResponse::from);
  }
}
