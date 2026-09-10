package org.civicops.grants;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.grants.grant.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GrantDomainTest {
  @Test
  void completeLifecycleIsAccepted() {
    Grant grant = grant();
    grant.transition(GrantStatus.APPLICATION_IN_PROGRESS, LocalDate.now());
    grant.transition(GrantStatus.SUBMITTED, LocalDate.now());
    grant.transition(GrantStatus.AWARDED, LocalDate.now());
    grant.transition(GrantStatus.ACTIVE, LocalDate.now());
    grant.transition(GrantStatus.CLOSED, LocalDate.now());
    assertThat(grant.getStatus()).isEqualTo(GrantStatus.CLOSED);
    assertThat(grant.getSubmittedDate()).isNotNull();
    assertThat(grant.getAwardDate()).isNotNull();
  }

  @Test
  void submittedGrantCanBeRejected() {
    Grant grant = grant();
    grant.transition(GrantStatus.APPLICATION_IN_PROGRESS, LocalDate.now());
    grant.transition(GrantStatus.SUBMITTED, LocalDate.now());
    grant.transition(GrantStatus.REJECTED, LocalDate.now());
    assertThat(grant.getStatus()).isEqualTo(GrantStatus.REJECTED);
  }

  @Test
  void applicationCanBeWithdrawn() {
    Grant grant = grant();
    grant.transition(GrantStatus.APPLICATION_IN_PROGRESS, LocalDate.now());
    grant.transition(GrantStatus.WITHDRAWN, LocalDate.now());
    assertThat(grant.getStatus()).isEqualTo(GrantStatus.WITHDRAWN);
  }

  @Test
  void prospectCanBeWithdrawn() {
    Grant grant = grant();
    grant.transition(GrantStatus.WITHDRAWN, LocalDate.now());
    assertThat(grant.getStatus()).isEqualTo(GrantStatus.WITHDRAWN);
  }

  @Test
  void closedGrantCannotReopen() {
    Grant grant = activeGrant();
    grant.transition(GrantStatus.CLOSED, LocalDate.now());
    assertThatThrownBy(() -> grant.transition(GrantStatus.ACTIVE, LocalDate.now()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("cannot transition");
  }

  @Test
  void rejectedGrantCannotActivate() {
    Grant grant = grant();
    grant.transition(GrantStatus.APPLICATION_IN_PROGRESS, LocalDate.now());
    grant.transition(GrantStatus.SUBMITTED, LocalDate.now());
    grant.transition(GrantStatus.REJECTED, LocalDate.now());
    assertThatThrownBy(() -> grant.transition(GrantStatus.ACTIVE, LocalDate.now()))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void withdrawnGrantCannotSubmit() {
    Grant grant = grant();
    grant.transition(GrantStatus.WITHDRAWN, LocalDate.now());
    assertThatThrownBy(() -> grant.transition(GrantStatus.SUBMITTED, LocalDate.now()))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void closedGrantCannotBePatched() {
    Grant grant = activeGrant();
    grant.transition(GrantStatus.CLOSED, LocalDate.now());
    assertThatThrownBy(
            () ->
                grant.update(
                    "Changed", null, null, null, null, null, null, null, null, null, null, null,
                    null, null))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("cannot be edited");
  }

  private Grant activeGrant() {
    Grant g = grant();
    g.transition(GrantStatus.APPLICATION_IN_PROGRESS, LocalDate.now());
    g.transition(GrantStatus.SUBMITTED, LocalDate.now());
    g.transition(GrantStatus.AWARDED, LocalDate.now());
    g.transition(GrantStatus.ACTIVE, LocalDate.now());
    return g;
  }

  private Grant grant() {
    return new Grant(
        Mockito.mock(Organization.class),
        Mockito.mock(User.class),
        "Grant",
        "Grantor",
        null,
        null,
        new BigDecimal("50000.00"),
        null,
        null,
        null,
        LocalDate.now(),
        LocalDate.now().plusMonths(6),
        LocalDate.now().plusMonths(7),
        false,
        null,
        null,
        null,
        null);
  }
}
