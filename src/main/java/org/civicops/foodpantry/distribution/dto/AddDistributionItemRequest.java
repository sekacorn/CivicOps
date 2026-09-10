package org.civicops.foodpantry.distribution.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record AddDistributionItemRequest(
    @NotNull UUID itemId,
    @NotNull @DecimalMin(value = "0.000", inclusive = false) @Digits(integer = 16, fraction = 3)
        BigDecimal quantity) {}
