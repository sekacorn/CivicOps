package org.civicops.facilities.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public record FacilityUtilizationResponse(
    UUID organizationId,
    LocalDate from,
    LocalDate to,
    long reservationCount,
    BigDecimal reservedHours,
    long approvals,
    long rejections,
    long cancellations,
    List<SpaceUsage> mostUsedSpaces) {
  public record SpaceUsage(
      UUID spaceId, String spaceName, long reservations, BigDecimal reservedHours) {}
}
