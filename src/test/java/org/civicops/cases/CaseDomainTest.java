package org.civicops.cases;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.time.*;
import org.civicops.cases.casefile.*;
import org.civicops.cases.client.Client;
import org.civicops.cases.task.*;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class CaseDomainTest {
  private final Organization org = mock(Organization.class);
  private final Client client = mock(Client.class);
  private final User user = mock(User.class);

  private CaseRecord record() {
    return new CaseRecord(
        org,
        client,
        user,
        "CASE-1",
        "Housing support",
        null,
        CaseType.HOUSING,
        CasePriority.HIGH,
        LocalDate.of(2026, 1, 1),
        null,
        null);
  }

  @Test
  void supportsRequiredLifecycleAndPopulatesClosedDate() {
    CaseRecord c = record();
    c.transition(CaseStatus.IN_PROGRESS, LocalDate.of(2026, 1, 2));
    c.transition(CaseStatus.ON_HOLD, LocalDate.of(2026, 1, 3));
    c.transition(CaseStatus.IN_PROGRESS, LocalDate.of(2026, 1, 4));
    c.transition(CaseStatus.CLOSED, LocalDate.of(2026, 1, 5));
    assertThat(c.getStatus()).isEqualTo(CaseStatus.CLOSED);
    assertThat(c.getClosedDate()).isEqualTo(LocalDate.of(2026, 1, 5));
  }

  @Test
  void terminalCaseRejectsEditingAssignmentAndInvalidTransition() {
    CaseRecord c = record();
    c.transition(CaseStatus.CANCELLED, LocalDate.now());
    assertThatThrownBy(() -> c.update("Changed", null, null, null, null, null))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> c.assign(user)).isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> c.transition(CaseStatus.IN_PROGRESS, LocalDate.now()))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void taskOverdueAndTerminalRulesAreExplicit() {
    CaseTask t =
        new CaseTask(
            org,
            record(),
            user,
            "Call client",
            null,
            user,
            LocalDate.of(2026, 1, 1),
            CasePriority.NORMAL);
    assertThat(t.isOverdue(LocalDate.of(2026, 1, 2))).isTrue();
    t.complete(Instant.parse("2026-01-02T12:00:00Z"));
    assertThat(t.isOverdue(LocalDate.of(2026, 1, 3))).isFalse();
    assertThatThrownBy(() -> t.cancel()).isInstanceOf(BusinessRuleException.class);
  }
}
