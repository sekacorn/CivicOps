package org.civicops.grants.expense;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.civicops.grants.expense.dto.*;
import org.civicops.grants.security.GrantAccessService;
import org.civicops.grants.support.GrantPageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/grants/{grantId}/expenses")
@Tag(name = "Grant Expenses")
public class GrantExpenseController {
  private final GrantExpenseService service;
  private final GrantAccessService access;

  public GrantExpenseController(GrantExpenseService service, GrantAccessService access) {
    this.service = service;
    this.access = access;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record an active-grant expense without overspending")
  public GrantExpenseResponse create(
      @PathVariable UUID organizationId,
      @PathVariable UUID grantId,
      @Valid @RequestBody CreateGrantExpenseRequest request) {
    access.requireManage(organizationId);
    return service.create(organizationId, grantId, access.userId(), request);
  }

  @GetMapping
  @Operation(
      summary = "List grant expenses",
      description = "Filters: category, from, to. Sort: expenseDate, amount, createdAt.")
  public Page<GrantExpenseResponse> list(
      @PathVariable UUID organizationId,
      @PathVariable UUID grantId,
      @RequestParam(required = false) ExpenseCategory category,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @PageableDefault(size = 20, sort = "expenseDate") Pageable pageable) {
    access.requireRead(organizationId);
    return service.list(
        organizationId,
        grantId,
        category,
        from,
        to,
        GrantPageables.allow(pageable, Set.of("expenseDate", "amount", "createdAt")));
  }
}
