package org.civicops.facilities.availability;

import java.time.*;
import java.util.*;
import org.civicops.facilities.availability.dto.AvailabilityResponse;
import org.civicops.facilities.reservation.*;
import org.civicops.facilities.space.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {
  private final FacilitySpaceService spaces;
  private final FacilityOperatingHoursRepository hours;
  private final FacilityBlackoutRepository blackouts;
  private final FacilityReservationRepository reservations;

  public AvailabilityService(
      FacilitySpaceService s,
      FacilityOperatingHoursRepository h,
      FacilityBlackoutRepository b,
      FacilityReservationRepository r) {
    spaces = s;
    hours = h;
    blackouts = b;
    reservations = r;
  }

  @Transactional(readOnly = true)
  public AvailabilityResponse check(UUID org, UUID spaceId, Instant start, Instant end) {
    return check(org, spaces.require(org, spaceId), start, end, new UUID(0, 0));
  }

  public AvailabilityResponse check(
      UUID org, FacilitySpace s, Instant start, Instant end, UUID exclude) {
    String reason = reason(org, s, start, end, exclude);
    return new AvailabilityResponse(s.getId(), start, end, reason == null, reason);
  }

  public void requireAvailable(
      UUID org, FacilitySpace s, Instant start, Instant end, UUID exclude) {
    String reason = reason(org, s, start, end, exclude);
    if (reason != null)
      throw new BusinessRuleException(reason, "Facility space is unavailable: " + reason);
  }

  private String reason(UUID org, FacilitySpace s, Instant start, Instant end, UUID exclude) {
    if (start == null || end == null || !end.isAfter(start)) return "INVALID_RESERVATION_TIME";
    if (!s.getFacility().isActive()) return "INACTIVE_FACILITY";
    if (!s.isActive()) return "INACTIVE_SPACE";
    if (!s.isReservable()) return "SPACE_NOT_RESERVABLE";
    ZoneId zone = ZoneId.of(s.getFacility().getTimezone());
    ZonedDateTime a = start.atZone(zone), b = end.atZone(zone);
    if (!a.toLocalDate().equals(b.toLocalDate())) return "OUTSIDE_OPERATING_HOURS";
    var h =
        hours
            .findByOrganizationIdAndFacilityIdAndDayOfWeek(
                org, s.getFacility().getId(), a.getDayOfWeek())
            .orElse(null);
    if (h == null
        || h.isClosed()
        || a.toLocalTime().isBefore(h.getOpenTime())
        || b.toLocalTime().isAfter(h.getCloseTime())) return "OUTSIDE_OPERATING_HOURS";
    if (blackouts.conflicts(org, s.getFacility().getId(), s.getId(), start, end) > 0)
      return "BLACKOUT_CONFLICT";
    if (reservations.approvedConflicts(org, s.getId(), exclude, start, end) > 0)
      return "RESERVATION_CONFLICT";
    return null;
  }
}
