package org.civicops.grants;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.civicops.core.organization.*;
import org.civicops.core.user.*;
import org.civicops.grants.expense.GrantExpenseRepository;
import org.civicops.grants.grant.*;
import org.civicops.grants.grant.dto.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.*;

class GrantServiceTest {
  GrantRepository grants = mock(GrantRepository.class);
  GrantExpenseRepository expenses = mock(GrantExpenseRepository.class);
  OrganizationService organizations = mock(OrganizationService.class);
  UserService users = mock(UserService.class);
  GrantService service = new GrantService(grants, expenses, organizations, users);
  UUID orgId = UUID.randomUUID(), userId = UUID.randomUUID(), grantId = UUID.randomUUID();

  @BeforeEach
  void setup() {
    when(organizations.requireEntity(orgId)).thenReturn(mock(Organization.class));
    when(users.requireEntity(userId)).thenReturn(mock(User.class));
    when(grants.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  void createsValidGrantWithExactMoneyScale() {
    var response = service.create(orgId, userId, create(new BigDecimal("50000"), false, null));
    assertThat(response.awardAmount()).isEqualByComparingTo("50000.00");
    assertThat(response.status()).isEqualTo(GrantStatus.PROSPECT);
    verify(grants).save(any(Grant.class));
  }

  @Test
  void rejectsInvalidDateRange() {
    var r =
        new CreateGrantRequest(
            "Grant",
            "Grantor",
            null,
            null,
            BigDecimal.TEN,
            null,
            null,
            null,
            LocalDate.now().plusDays(2),
            LocalDate.now(),
            null,
            false,
            null,
            null,
            null,
            null);
    assertThatThrownBy(() -> service.create(orgId, userId, r))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void requiresRestrictionDescription() {
    assertThatThrownBy(() -> service.create(orgId, userId, create(BigDecimal.TEN, true, " ")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Restriction description");
  }

  @Test
  void lifecycleDatesCannotBeInjectedAtCreation() {
    var r =
        new CreateGrantRequest(
            "Grant",
            "Grantor",
            null,
            null,
            BigDecimal.TEN,
            null,
            LocalDate.now(),
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            null);
    assertThatThrownBy(() -> service.create(orgId, userId, r))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void awardCannotBeReducedBelowSpending() {
    Grant grant = grant(new BigDecimal("50000.00"));
    when(grants.findLockedByIdAndOrganizationId(grantId, orgId)).thenReturn(Optional.of(grant));
    when(expenses.totalForGrant(grantId)).thenReturn(new BigDecimal("22000.00"));
    assertThatThrownBy(() -> service.update(orgId, grantId, updateAward("20000.00")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("recorded grant expenditure");
  }

  @Test
  void awardCanBeReducedToAmountAboveSpending() {
    Grant grant = grant(new BigDecimal("50000.00"));
    when(grants.findLockedByIdAndOrganizationId(grantId, orgId)).thenReturn(Optional.of(grant));
    when(expenses.totalForGrant(grantId)).thenReturn(new BigDecimal("22000.00"));
    var result = service.update(orgId, grantId, updateAward("30000.00"));
    assertThat(result.awardAmount()).isEqualByComparingTo("30000.00");
  }

  @Test
  void omittedPatchFieldsArePreserved() {
    Grant grant = grant(new BigDecimal("50000.00"));
    when(grants.findLockedByIdAndOrganizationId(grantId, orgId)).thenReturn(Optional.of(grant));
    when(expenses.totalForGrant(grantId)).thenReturn(BigDecimal.ZERO);
    var result =
        service.update(
            orgId,
            grantId,
            new UpdateGrantRequest(
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null));
    assertThat(result.grantName()).isEqualTo("Original");
    assertThat(result.awardAmount()).isEqualByComparingTo("50000.00");
  }

  private CreateGrantRequest create(BigDecimal amount, boolean restricted, String restriction) {
    return new CreateGrantRequest(
        "Grant",
        "Grantor",
        null,
        null,
        amount,
        null,
        null,
        null,
        LocalDate.now(),
        LocalDate.now().plusMonths(1),
        LocalDate.now().plusMonths(2),
        restricted,
        restriction,
        null,
        null,
        null);
  }

  private UpdateGrantRequest updateAward(String value) {
    return new UpdateGrantRequest(
        null,
        null,
        null,
        null,
        new BigDecimal(value),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private Grant grant(BigDecimal amount) {
    Organization org = mock(Organization.class);
    User user = mock(User.class);
    when(org.getId()).thenReturn(orgId);
    when(user.getId()).thenReturn(userId);
    return new Grant(
        org,
        user,
        "Original",
        "Grantor",
        null,
        null,
        amount,
        null,
        null,
        null,
        LocalDate.now(),
        LocalDate.now().plusMonths(1),
        LocalDate.now().plusMonths(2),
        false,
        null,
        null,
        null,
        null);
  }
}
