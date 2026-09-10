package org.civicops.grants.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.UserService;
import org.civicops.grants.expense.dto.*;
import org.civicops.grants.grant.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrantExpenseService {
  private final GrantExpenseRepository expenses;
  private final GrantRepository grants;
  private final OrganizationService organizations;
  private final UserService users;

  public GrantExpenseService(
      GrantExpenseRepository expenses,
      GrantRepository grants,
      OrganizationService organizations,
      UserService users) {
    this.expenses = expenses;
    this.grants = grants;
    this.organizations = organizations;
    this.users = users;
  }

  @Transactional
  public GrantExpenseResponse create(
      UUID organizationId, UUID grantId, UUID userId, CreateGrantExpenseRequest request) {
    Grant grant =
        grants
            .findLockedByIdAndOrganizationId(grantId, organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Grant", grantId));
    if (grant.getStatus() != GrantStatus.ACTIVE) {
      throw new BusinessRuleException(
          "GRANT_NOT_OPEN_FOR_EXPENSES", "Expenses may only be recorded against an active grant");
    }
    BigDecimal amount = GrantService.money(request.amount());
    BigDecimal projected = expenses.totalForGrant(grantId).add(amount);
    if (projected.compareTo(grant.getAwardAmount()) > 0) {
      throw new BusinessRuleException(
          "GRANT_AWARD_EXCEEDED",
          "Expense would cause recorded spending to exceed the grant award");
    }
    GrantExpense expense =
        new GrantExpense(
            organizations.requireEntity(organizationId),
            grant,
            users.requireEntity(userId),
            amount,
            request.expenseDate(),
            request.category(),
            request.description().trim(),
            clean(request.vendor()),
            clean(request.referenceNumber()),
            clean(request.notes()));
    return GrantExpenseResponse.from(expenses.save(expense));
  }

  @Transactional(readOnly = true)
  public GrantExpenseResponse detail(UUID organizationId, UUID expenseId) {
    return GrantExpenseResponse.from(
        expenses
            .findByIdAndOrganizationId(expenseId, organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("GrantExpense", expenseId)));
  }

  @Transactional(readOnly = true)
  public Page<GrantExpenseResponse> list(
      UUID organizationId,
      UUID grantId,
      ExpenseCategory category,
      LocalDate from,
      LocalDate to,
      Pageable pageable) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException(
          "INVALID_DATE_RANGE", "Expense filter end cannot precede start");
    if (!grants.findByIdAndOrganizationId(grantId, organizationId).isPresent())
      throw new ResourceNotFoundException("Grant", grantId);
    Specification<GrantExpense> spec =
        (root, query, cb) ->
            cb.and(
                cb.equal(root.get("organization").get("id"), organizationId),
                cb.equal(root.get("grant").get("id"), grantId));
    if (category != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("category"), category));
    if (from != null)
      spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("expenseDate"), from));
    if (to != null) spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("expenseDate"), to));
    return expenses.findAll(spec, pageable).map(GrantExpenseResponse::from);
  }

  private static String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
