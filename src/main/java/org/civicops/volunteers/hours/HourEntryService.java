package org.civicops.volunteers.hours;

import java.time.*;
import java.util.*;
import org.civicops.core.user.*;
import org.civicops.shared.exception.*;
import org.civicops.volunteers.assignment.*;
import org.civicops.volunteers.hours.dto.*;
import org.civicops.volunteers.volunteer.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HourEntryService {
  private final VolunteerHourEntryRepository hours;
  private final VolunteerRepository volunteers;
  private final VolunteerAssignmentRepository assignments;
  private final UserService users;
  private final Clock clock;

  public HourEntryService(
      VolunteerHourEntryRepository h,
      VolunteerRepository v,
      VolunteerAssignmentRepository a,
      UserService u,
      Clock c) {
    hours = h;
    volunteers = v;
    assignments = a;
    users = u;
    clock = c;
  }

  @Transactional
  public HourEntryResponse submit(UUID orgId, CreateHourEntryRequest r) {
    Volunteer v =
        volunteers
            .findByIdAndOrganizationId(r.volunteerId(), orgId)
            .orElseThrow(() -> new ResourceNotFoundException("Volunteer", r.volunteerId()));
    VolunteerAssignment a = null;
    if (r.assignmentId() != null) {
      a =
          assignments
              .findByIdAndOrganizationId(r.assignmentId(), orgId)
              .orElseThrow(
                  () -> new ResourceNotFoundException("VolunteerAssignment", r.assignmentId()));
      if (!a.getVolunteer().getId().equals(v.getId()))
        throw new BusinessRuleException(
            "ASSIGNMENT_VOLUNTEER_MISMATCH", "Hour entry assignment belongs to another volunteer");
    }
    return HourEntryResponse.from(
        hours.save(
            new VolunteerHourEntry(
                v.getOrganization(), v, a, r.serviceDate(), r.hours(), clean(r.description()))));
  }

  @Transactional(readOnly = true)
  public VolunteerHourEntry require(UUID orgId, UUID id) {
    return hours
        .findByIdAndOrganizationId(id, orgId)
        .orElseThrow(() -> new ResourceNotFoundException("VolunteerHourEntry", id));
  }

  @Transactional
  public HourEntryResponse approve(UUID orgId, UUID id, UUID reviewerId) {
    VolunteerHourEntry h = require(orgId, id);
    preventSelf(h, reviewerId);
    h.approve(users.requireEntity(reviewerId), clock.instant());
    return HourEntryResponse.from(h);
  }

  @Transactional
  public HourEntryResponse reject(UUID orgId, UUID id, UUID reviewerId, String reason) {
    VolunteerHourEntry h = require(orgId, id);
    preventSelf(h, reviewerId);
    h.reject(users.requireEntity(reviewerId), clock.instant(), reason.trim());
    return HourEntryResponse.from(h);
  }

  @Transactional(readOnly = true)
  public Page<HourEntryResponse> list(
      UUID orgId,
      HourEntryStatus status,
      UUID volunteerId,
      LocalDate from,
      LocalDate to,
      Pageable p) {
    if (from != null && to != null && to.isBefore(from)) {
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Filter end must not precede start");
    }
    Specification<VolunteerHourEntry> spec =
        (root, query, cb) -> cb.equal(root.get("organization").get("id"), orgId);
    if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    if (volunteerId != null)
      spec = spec.and((root, query, cb) -> cb.equal(root.get("volunteer").get("id"), volunteerId));
    if (from != null)
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("serviceDate"), from));
    if (to != null)
      spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("serviceDate"), to));
    return hours.findAll(spec, p).map(HourEntryResponse::from);
  }

  @Transactional(readOnly = true)
  public Page<HourEntryResponse> forVolunteer(UUID orgId, UUID volunteerId, Pageable p) {
    return hours
        .findByOrganizationIdAndVolunteerId(orgId, volunteerId, p)
        .map(HourEntryResponse::from);
  }

  private void preventSelf(VolunteerHourEntry h, UUID reviewerId) {
    if (h.getVolunteer().getUser() != null && h.getVolunteer().getUser().getId().equals(reviewerId))
      throw new BusinessRuleException(
          "SELF_APPROVAL_NOT_ALLOWED", "Volunteers cannot review their own hours");
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
