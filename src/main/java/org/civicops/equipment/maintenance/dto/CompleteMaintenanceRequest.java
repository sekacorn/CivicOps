package org.civicops.equipment.maintenance.dto;

import jakarta.validation.constraints.NotNull;
import org.civicops.equipment.asset.EquipmentCondition;

public record CompleteMaintenanceRequest(@NotNull EquipmentCondition resultingCondition) {}
