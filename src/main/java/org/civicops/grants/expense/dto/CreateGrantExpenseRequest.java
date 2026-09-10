package org.civicops.grants.expense.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.civicops.grants.expense.ExpenseCategory;

public record CreateGrantExpenseRequest(
    @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 17, fraction = 2)
        BigDecimal amount,
    @NotNull LocalDate expenseDate,
    @NotNull ExpenseCategory category,
    @NotBlank @Size(max = 500) String description,
    @Size(max = 200) String vendor,
    @Size(max = 100) String referenceNumber,
    String notes) {}
