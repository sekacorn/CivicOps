package org.civicops.grants.expense;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.civicops.grants.expense.dto.GrantExpenseResponse;
import org.civicops.grants.security.GrantAccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/grant-expenses")
@Tag(name = "Grant Expenses")
public class GrantExpenseDetailController {
  private final GrantExpenseService service;
  private final GrantAccessService access;

  public GrantExpenseDetailController(GrantExpenseService service, GrantAccessService access) {
    this.service = service;
    this.access = access;
  }

  @GetMapping("/{expenseId}")
  public GrantExpenseResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID expenseId) {
    access.requireManage(organizationId);
    return service.detail(organizationId, expenseId);
  }
}
