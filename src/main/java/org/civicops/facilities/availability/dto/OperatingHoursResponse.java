package org.civicops.facilities.availability.dto;

import java.time.*;
import java.util.UUID;
import org.civicops.facilities.availability.FacilityOperatingHours;

public record OperatingHoursResponse(
    UUID id, DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {
  public static OperatingHoursResponse from(FacilityOperatingHours h) {
    return new OperatingHoursResponse(
        h.getId(), h.getDayOfWeek(), h.getOpenTime(), h.getCloseTime(), h.isClosed());
  }
}
