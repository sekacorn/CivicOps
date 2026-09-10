package org.civicops.foodpantry.reporting.dto;

import java.math.BigDecimal;
import org.civicops.foodpantry.item.FoodCategory;

public record CategoryDistributionQuantity(FoodCategory category, BigDecimal quantity) {}
