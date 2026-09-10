package org.civicops.grants;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.civicops.core.organization.*;
import org.civicops.core.user.*;
import org.civicops.grants.expense.*;
import org.civicops.grants.expense.dto.*;
import org.civicops.grants.grant.*;
import org.civicops.shared.exception.*;
import org.junit.jupiter.api.*;

class GrantExpenseServiceTest {
  GrantExpenseRepository expenses = mock(GrantExpenseRepository.class);
  GrantRepository grants = mock(GrantRepository.class);
  OrganizationService organizations = mock(OrganizationService.class);
  UserService users = mock(UserService.class);
  GrantExpenseService service = new GrantExpenseService(expenses, grants, organizations, users);
  UUID orgId = UUID.randomUUID(), grantId = UUID.randomUUID(), userId = UUID.randomUUID();

  @BeforeEach
  void setup() {
    when(expenses.save(any())).thenAnswer(i -> i.getArgument(0));
    when(organizations.requireEntity(orgId)).thenReturn(mock(Organization.class));
    when(users.requireEntity(userId)).thenReturn(mock(User.class));
  }

  @Test
  void recordsValidExpenseForActiveGrant() {
    activeGrant("10000.00");
    when(expenses.totalForGrant(grantId)).thenReturn(new BigDecimal("9500.00"));
    var response = service.create(orgId, grantId, userId, request("499.00"));
    assertThat(response.amount()).isEqualByComparingTo("499.00");
    verify(expenses).save(any(GrantExpense.class));
  }

  @Test
  void rejectsOverspending() {
    activeGrant("10000.00");
    when(expenses.totalForGrant(grantId)).thenReturn(new BigDecimal("9500.00"));
    assertThatThrownBy(() -> service.create(orgId, grantId, userId, request("501.00")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("exceed the grant award");
  }

  @Test
  void rejectsExpenseUnlessGrantIsActive() {
    Grant grant = mock(Grant.class);
    when(grant.getStatus()).thenReturn(GrantStatus.AWARDED);
    when(grants.findLockedByIdAndOrganizationId(grantId, orgId)).thenReturn(Optional.of(grant));
    assertThatThrownBy(() -> service.create(orgId, grantId, userId, request("10.00")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("active grant");
  }

  @Test
  void closedGrantRejectsExpense() {
    Grant grant = mock(Grant.class);
    when(grant.getStatus()).thenReturn(GrantStatus.CLOSED);
    when(grants.findLockedByIdAndOrganizationId(grantId, orgId)).thenReturn(Optional.of(grant));
    assertThatThrownBy(() -> service.create(orgId, grantId, userId, request("10.00")))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void missingOrCrossOrganizationGrantIsNotFound() {
    when(grants.findLockedByIdAndOrganizationId(grantId, orgId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.create(orgId, grantId, userId, request("10.00")))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  private void activeGrant(String award) {
    Grant grant = mock(Grant.class);
    when(grant.getStatus()).thenReturn(GrantStatus.ACTIVE);
    when(grant.getAwardAmount()).thenReturn(new BigDecimal(award));
    when(grant.getId()).thenReturn(grantId);
    when(grants.findLockedByIdAndOrganizationId(grantId, orgId)).thenReturn(Optional.of(grant));
  }

  private CreateGrantExpenseRequest request(String amount) {
    return new CreateGrantExpenseRequest(
        new BigDecimal(amount),
        LocalDate.now(),
        ExpenseCategory.SUPPLIES,
        "Program supplies",
        null,
        null,
        null);
  }
}
