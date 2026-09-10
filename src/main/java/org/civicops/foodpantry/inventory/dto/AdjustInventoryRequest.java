package org.civicops.foodpantry.inventory.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.civicops.foodpantry.inventory.InventoryAdjustmentType;

public record AdjustInventoryRequest(
    @NotNull InventoryAdjustmentType adjustmentType,
    @NotNull @DecimalMin(value = "0.000", inclusive = false) @Digits(integer = 16, fraction = 3)
        BigDecimal quantity,
    @NotBlank @Size(max = 1000) String reason) {}
