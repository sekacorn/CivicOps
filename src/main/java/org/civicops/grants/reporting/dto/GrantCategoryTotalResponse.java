package org.civicops.grants.reporting.dto;

import java.math.BigDecimal;
import org.civicops.grants.expense.ExpenseCategory;

public record GrantCategoryTotalResponse(ExpenseCategory category, BigDecimal amount) {}
