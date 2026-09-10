package org.civicops.facilities.space;

import java.util.*;
import org.civicops.facilities.facility.*;
import org.civicops.facilities.space.dto.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacilitySpaceService {
  private final FacilitySpaceRepository spaces;
  private final FacilityService facilities;

  public FacilitySpaceService(FacilitySpaceRepository s, FacilityService f) {
    spaces = s;
    facilities = f;
  }

  @Transactional
  public FacilitySpaceResponse create(UUID org, UUID facilityId, CreateFacilitySpaceRequest r) {
    Facility f = facilities.require(org, facilityId);
    if (!f.isActive())
      throw new BusinessRuleException(
          "INACTIVE_FACILITY", "Spaces cannot be added to an inactive facility");
    String norm = normalize(r.name());
    if (spaces.existsByFacilityIdAndNormalizedName(facilityId, norm))
      throw new ConflictException(
          "DUPLICATE_SPACE_NAME", "Space name already exists in this facility");
    return FacilitySpaceResponse.from(
        spaces.save(
            new FacilitySpace(
                f,
                r.name().trim(),
                norm,
                clean(r.description()),
                r.capacity(),
                r.reservable() == null || r.reservable(),
                clean(r.locationDetails()),
                clean(r.accessibilityNotes()))));
  }

  @Transactional(readOnly = true)
  public FacilitySpace require(UUID org, UUID id) {
    return spaces
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Facility space", id));
  }

  @Transactional
  public FacilitySpace requireLocked(UUID org, UUID id) {
    return spaces
        .findLocked(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Facility space", id));
  }

  @Transactional
  public FacilitySpaceResponse update(UUID org, UUID id, UpdateFacilitySpaceRequest r) {
    FacilitySpace s = require(org, id);
    String norm = r.name() == null ? null : normalize(r.name());
    if (norm != null
        && !norm.equals(s.getNormalizedName())
        && spaces.existsByFacilityIdAndNormalizedName(s.getFacility().getId(), norm))
      throw new ConflictException(
          "DUPLICATE_SPACE_NAME", "Space name already exists in this facility");
    s.update(
        clean(r.name()),
        norm,
        clean(r.description()),
        r.capacity(),
        r.reservable(),
        r.active(),
        clean(r.locationDetails()),
        clean(r.accessibilityNotes()));
    return FacilitySpaceResponse.from(s);
  }

  @Transactional(readOnly = true)
  public FacilitySpaceResponse detail(UUID org, UUID id) {
    return FacilitySpaceResponse.from(require(org, id));
  }

  @Transactional(readOnly = true)
  public Page<FacilitySpaceResponse> list(
      UUID org,
      UUID facility,
      Boolean active,
      Boolean reservable,
      Integer minCapacity,
      Pageable p) {
    Specification<FacilitySpace> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (facility != null) s = s.and((r, q, c) -> c.equal(r.get("facility").get("id"), facility));
    if (active != null) s = s.and((r, q, c) -> c.equal(r.get("active"), active));
    if (reservable != null) s = s.and((r, q, c) -> c.equal(r.get("reservable"), reservable));
    if (minCapacity != null)
      s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("capacity"), minCapacity));
    return spaces.findAll(s, p).map(FacilitySpaceResponse::from);
  }

  public static String normalize(String x) {
    return x.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
