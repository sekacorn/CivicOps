package org.civicops.grants.expense.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.civicops.grants.expense.*;

public record GrantExpenseResponse(
    UUID id,
    UUID grantId,
    BigDecimal amount,
    LocalDate expenseDate,
    ExpenseCategory category,
    String description,
    String vendor,
    String referenceNumber,
    String notes,
    UUID createdByUserId,
    Instant createdAt,
    Instant updatedAt) {
  public static GrantExpenseResponse from(GrantExpense e) {
    return new GrantExpenseResponse(
        e.getId(),
        e.getGrant().getId(),
        e.getAmount(),
        e.getExpenseDate(),
        e.getCategory(),
        e.getDescription(),
        e.getVendor(),
        e.getReferenceNumber(),
        e.getNotes(),
        e.getCreatedBy().getId(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }
}
