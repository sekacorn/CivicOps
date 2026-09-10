package org.civicops.facilities.availability;

import java.time.*;
import java.util.*;
import org.civicops.core.user.UserService;
import org.civicops.facilities.availability.dto.*;
import org.civicops.facilities.facility.*;
import org.civicops.facilities.space.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacilityBlackoutService {
  private final FacilityBlackoutRepository blackouts;
  private final FacilityService facilities;
  private final FacilitySpaceService spaces;
  private final UserService users;
  private final Clock clock;

  public FacilityBlackoutService(
      FacilityBlackoutRepository b,
      FacilityService f,
      FacilitySpaceService s,
      UserService u,
      Clock c) {
    blackouts = b;
    facilities = f;
    spaces = s;
    users = u;
    clock = c;
  }

  @Transactional
  public BlackoutResponse create(UUID org, UUID actor, CreateBlackoutRequest r) {
    if (!r.endDateTime().isAfter(r.startDateTime()))
      throw new BusinessRuleException("INVALID_BLACKOUT_TIME", "Blackout end must be after start");
    Facility f = facilities.require(org, r.facilityId());
    FacilitySpace s = r.facilitySpaceId() == null ? null : spaces.require(org, r.facilitySpaceId());
    if (s != null && !s.getFacility().getId().equals(f.getId()))
      throw new BusinessRuleException(
          "BLACKOUT_SPACE_MISMATCH", "Blackout space must belong to facility");
    return BlackoutResponse.from(
        blackouts.save(
            new FacilityBlackout(
                f,
                s,
                r.startDateTime(),
                r.endDateTime(),
                r.reason().trim(),
                users.requireEntity(actor))));
  }

  @Transactional
  public BlackoutResponse cancel(UUID org, UUID id) {
    FacilityBlackout b = require(org, id);
    b.cancel(Instant.now(clock));
    return BlackoutResponse.from(b);
  }

  @Transactional(readOnly = true)
  public FacilityBlackout require(UUID org, UUID id) {
    return blackouts
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Facility blackout", id));
  }

  @Transactional(readOnly = true)
  public Page<BlackoutResponse> list(
      UUID org, UUID facility, UUID space, Instant from, Instant to, Pageable p) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Filter end precedes start");
    Specification<FacilityBlackout> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (facility != null) s = s.and((r, q, c) -> c.equal(r.get("facility").get("id"), facility));
    if (space != null) s = s.and((r, q, c) -> c.equal(r.get("facilitySpace").get("id"), space));
    if (from != null) s = s.and((r, q, c) -> c.greaterThan(r.get("endDateTime"), from));
    if (to != null) s = s.and((r, q, c) -> c.lessThan(r.get("startDateTime"), to));
    return blackouts.findAll(s, p).map(BlackoutResponse::from);
  }
}
