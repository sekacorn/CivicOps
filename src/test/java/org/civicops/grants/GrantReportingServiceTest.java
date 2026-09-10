package org.civicops.grants;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.civicops.grants.expense.*;
import org.civicops.grants.grant.*;
import org.civicops.grants.reporting.GrantReportingService;
import org.junit.jupiter.api.Test;

class GrantReportingServiceTest {
  GrantRepository grants = mock(GrantRepository.class);
  GrantExpenseRepository expenses = mock(GrantExpenseRepository.class);
  GrantReportingService service = new GrantReportingService(grants, expenses);
  UUID orgId = UUID.randomUUID(), grantId = UUID.randomUUID();

  @Test
  void calculatesExactFinancialSummary() {
    Grant g = grant(GrantStatus.ACTIVE, "50000.00", null);
    when(grants.findByIdAndOrganizationId(grantId, orgId)).thenReturn(Optional.of(g));
    when(expenses.totalForGrant(grantId)).thenReturn(new BigDecimal("5700.00"));
    var result = service.financial(orgId, grantId);
    assertThat(result.totalSpent()).isEqualByComparingTo("5700.00");
    assertThat(result.remainingBalance()).isEqualByComparingTo("44300.00");
    assertThat(result.utilizationPercent()).isEqualByComparingTo("11.40");
  }

  @Test
  void zeroAwardAndExpensesHaveZeroUtilization() {
    Grant g = grant(GrantStatus.AWARDED, "0.00", null);
    when(grants.findByIdAndOrganizationId(grantId, orgId)).thenReturn(Optional.of(g));
    when(expenses.totalForGrant(grantId)).thenReturn(BigDecimal.ZERO);
    assertThat(service.financial(orgId, grantId).utilizationPercent()).isEqualByComparingTo("0.00");
  }

  @Test
  void utilizationRoundsHalfUpToTwoDecimals() {
    Grant g = grant(GrantStatus.ACTIVE, "3.00", null);
    when(grants.findByIdAndOrganizationId(grantId, orgId)).thenReturn(Optional.of(g));
    when(expenses.totalForGrant(grantId)).thenReturn(new BigDecimal("1.00"));
    assertThat(service.financial(orgId, grantId).utilizationPercent())
        .isEqualByComparingTo("33.33");
  }

  @Test
  void emptyOrganizationReturnsZeros() {
    when(grants.findAllByOrganizationId(orgId)).thenReturn(List.of());
    var result = service.summary(orgId);
    assertThat(result.totalGrants()).isZero();
    assertThat(result.totalAwarded()).isEqualByComparingTo("0.00");
    assertThat(result.averageUtilizationPercent()).isEqualByComparingTo("0.00");
  }

  @Test
  void categoryAggregationUsesRepositoryTotals() {
    Grant g = grant(GrantStatus.ACTIVE, "50000", null);
    when(grants.findByIdAndOrganizationId(grantId, orgId)).thenReturn(Optional.of(g));
    when(expenses.totalsByCategory(orgId, grantId))
        .thenReturn(
            List.of(
                new Object[] {ExpenseCategory.SUPPLIES, new BigDecimal("4200.00")},
                new Object[] {ExpenseCategory.TRANSPORTATION, new BigDecimal("1500.00")}));
    assertThat(service.byCategory(orgId, grantId))
        .extracting("amount")
        .containsExactly(new BigDecimal("4200.00"), new BigDecimal("1500.00"));
  }

  private Grant grant(GrantStatus status, String award, LocalDate deadline) {
    Grant g = mock(Grant.class);
    when(g.getId()).thenReturn(grantId);
    when(g.getStatus()).thenReturn(status);
    when(g.getAwardAmount()).thenReturn(new BigDecimal(award));
    when(g.getReportingDeadline()).thenReturn(deadline);
    return g;
  }
}
