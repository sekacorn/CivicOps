package org.civicops.facilities.availability;

import java.time.*;
import java.util.*;
import org.civicops.facilities.availability.dto.*;
import org.civicops.facilities.facility.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperatingHoursService {
  private final FacilityOperatingHoursRepository hours;
  private final FacilityService facilities;

  public OperatingHoursService(FacilityOperatingHoursRepository h, FacilityService f) {
    hours = h;
    facilities = f;
  }

  @Transactional
  public List<OperatingHoursResponse> replace(
      UUID org, UUID facilityId, List<OperatingHoursRequest> requests) {
    Facility f = facilities.require(org, facilityId);
    Set<DayOfWeek> seen = EnumSet.noneOf(DayOfWeek.class);
    for (var r : requests) {
      if (!seen.add(r.dayOfWeek()))
        throw new BusinessRuleException(
            "DUPLICATE_OPERATING_DAY", "Operating-hours request contains a duplicate day");
      validate(r);
      var h =
          hours
              .findByOrganizationIdAndFacilityIdAndDayOfWeek(org, facilityId, r.dayOfWeek())
              .orElseGet(
                  () ->
                      new FacilityOperatingHours(
                          f, r.dayOfWeek(), r.openTime(), r.closeTime(), r.closed()));
      h.set(r.openTime(), r.closeTime(), r.closed());
      hours.save(h);
    }
    return list(org, facilityId);
  }

  @Transactional(readOnly = true)
  public List<OperatingHoursResponse> list(UUID org, UUID facilityId) {
    facilities.require(org, facilityId);
    return hours.findAllByOrganizationIdAndFacilityIdOrderByDayOfWeek(org, facilityId).stream()
        .map(OperatingHoursResponse::from)
        .toList();
  }

  private static void validate(OperatingHoursRequest r) {
    if (r.closed()) {
      if (r.openTime() != null || r.closeTime() != null)
        throw new BusinessRuleException(
            "INVALID_OPERATING_HOURS", "Closed days must not define open or close times");
    } else if (r.openTime() == null
        || r.closeTime() == null
        || !r.closeTime().isAfter(r.openTime()))
      throw new BusinessRuleException(
          "INVALID_OPERATING_HOURS", "Open days require close time after open time");
  }
}
