package org.civicops.equipment.reporting.dto;

import java.util.UUID;

public record EquipmentInventorySummaryResponse(
    UUID organizationId,
    long totalAssets,
    long availableAssets,
    long checkedOutAssets,
    long maintenanceAssets,
    long lostAssets,
    long retiredAssets,
    long overdueCheckouts) {}
