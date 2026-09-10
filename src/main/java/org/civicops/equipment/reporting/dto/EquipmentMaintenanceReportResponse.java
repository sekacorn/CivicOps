package org.civicops.equipment.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.civicops.equipment.maintenance.MaintenanceType;

public record EquipmentMaintenanceReportResponse(
    UUID organizationId,
    LocalDate from,
    LocalDate to,
    long openMaintenance,
    Map<MaintenanceType, Long> recordsByType,
    BigDecimal totalCost) {}
