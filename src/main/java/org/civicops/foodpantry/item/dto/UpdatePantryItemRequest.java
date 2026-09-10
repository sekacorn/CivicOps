package org.civicops.foodpantry.item.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.civicops.foodpantry.item.*;

public record UpdatePantryItemRequest(
    @Size(max = 100) String sku,
    @Size(max = 200) String name,
    @Size(max = 5000) String description,
    FoodCategory category,
    UnitType unitType,
    Boolean trackExpiration,
    @DecimalMin("0.000") @Digits(integer = 16, fraction = 3) BigDecimal reorderThreshold,
    @Size(max = 5000) String notes) {}
