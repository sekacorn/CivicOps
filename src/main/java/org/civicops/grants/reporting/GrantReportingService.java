package org.civicops.grants.reporting;

import java.math.*;
import java.time.LocalDate;
import java.util.*;
import org.civicops.grants.expense.*;
import org.civicops.grants.grant.*;
import org.civicops.grants.reporting.dto.*;
import org.civicops.shared.exception.ResourceNotFoundException;
import org.civicops.shared.finance.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrantReportingService {
  private static final Set<GrantStatus> AWARDED =
      EnumSet.of(GrantStatus.AWARDED, GrantStatus.ACTIVE, GrantStatus.CLOSED);
  private final GrantRepository grants;
  private final GrantExpenseRepository expenses;

  public GrantReportingService(GrantRepository grants, GrantExpenseRepository expenses) {
    this.grants = grants;
    this.expenses = expenses;
  }

  @Transactional(readOnly = true)
  public GrantFinancialSummaryResponse financial(UUID organizationId, UUID grantId) {
    Grant grant =
        grants
            .findByIdAndOrganizationId(grantId, organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Grant", grantId));
    BigDecimal spent = money(expenses.totalForGrant(grantId));
    return financial(grant, spent);
  }

  @Transactional(readOnly = true)
  public List<GrantCategoryTotalResponse> byCategory(UUID organizationId, UUID grantId) {
    if (grants.findByIdAndOrganizationId(grantId, organizationId).isEmpty())
      throw new ResourceNotFoundException("Grant", grantId);
    return expenses.totalsByCategory(organizationId, grantId).stream()
        .map(
            row ->
                new GrantCategoryTotalResponse(
                    (ExpenseCategory) row[0], money((BigDecimal) row[1])))
        .toList();
  }

  @Transactional(readOnly = true)
  public OrganizationGrantSummaryResponse summary(UUID organizationId) {
    List<Grant> all = grants.findAllByOrganizationId(organizationId);
    List<Grant> awarded = all.stream().filter(g -> AWARDED.contains(g.getStatus())).toList();
    BigDecimal totalAwarded =
        awarded.stream().map(Grant::getAwardAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalSpent =
        awarded.stream()
            .map(g -> expenses.totalForGrant(g.getId()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal average =
        awarded.isEmpty()
            ? BigDecimal.ZERO.setScale(2)
            : awarded.stream()
                .map(g -> utilization(expenses.totalForGrant(g.getId()), g.getAwardAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(awarded.size()), 2, RoundingMode.HALF_UP);
    LocalDate today = LocalDate.now();
    long dueSoon =
        awarded.stream()
            .filter(g -> g.getReportingDeadline() != null)
            .filter(
                g ->
                    !g.getReportingDeadline().isBefore(today)
                        && !g.getReportingDeadline().isAfter(today.plusDays(30)))
            .count();
    return new OrganizationGrantSummaryResponse(
        all.size(),
        all.stream().filter(g -> g.getStatus() == GrantStatus.ACTIVE).count(),
        money(totalAwarded),
        money(totalSpent),
        money(totalAwarded.subtract(totalSpent)),
        average,
        dueSoon);
  }

  private static GrantFinancialSummaryResponse financial(Grant grant, BigDecimal spent) {
    BigDecimal award = money(grant.getAwardAmount());
    return new GrantFinancialSummaryResponse(
        grant.getId(), award, spent, money(award.subtract(spent)), utilization(spent, award));
  }

  private static BigDecimal utilization(BigDecimal spent, BigDecimal award) {
    return Money.percent(spent, award);
  }

  private static BigDecimal money(BigDecimal value) {
    return Money.amount(value);
  }
}
