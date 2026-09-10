package org.civicops.equipment.reporting.dto;

import java.time.*;
import java.util.*;

public record EquipmentUtilizationResponse(
    UUID organizationId,
    LocalDate from,
    LocalDate to,
    long checkoutCount,
    List<AssetCheckoutCount> mostFrequentlyCheckedOut,
    long currentlyOverdue,
    long currentlyInMaintenance) {
  public record AssetCheckoutCount(
      UUID assetId, String assetTag, String assetName, long checkoutCount) {}
}
