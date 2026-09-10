package org.civicops.equipment.asset.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.civicops.equipment.asset.EquipmentCondition;

public record UpdateEquipmentAssetRequest(
    @Size(max = 200) String name,
    @Size(max = 10000) String description,
    UUID categoryId,
    @Size(max = 150) String manufacturer,
    @Size(max = 150) String model,
    @Size(max = 150) String serialNumber,
    LocalDate purchaseDate,
    @PositiveOrZero BigDecimal purchaseValue,
    EquipmentCondition condition,
    @Size(max = 250) String location,
    @Size(max = 10000) String notes) {}
