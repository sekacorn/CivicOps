package org.civicops.foodpantry.inventory.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.civicops.foodpantry.inventory.InventorySourceType;

public record CreateInventoryReceiptRequest(
    @NotNull UUID itemId,
    @NotNull @DecimalMin(value = "0.000", inclusive = false) @Digits(integer = 16, fraction = 3)
        BigDecimal quantity,
    @NotNull LocalDate receivedDate,
    @Size(max = 100) String lotNumber,
    LocalDate expirationDate,
    InventorySourceType sourceType,
    @Size(max = 200) String sourceReference,
    @Size(max = 5000) String notes) {}
