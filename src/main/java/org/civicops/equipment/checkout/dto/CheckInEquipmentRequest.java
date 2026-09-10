package org.civicops.equipment.checkout.dto;

import jakarta.validation.constraints.*;
import org.civicops.equipment.asset.EquipmentCondition;

public record CheckInEquipmentRequest(
    @NotNull EquipmentCondition returnCondition, @Size(max = 10000) String notes) {}
