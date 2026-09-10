package org.civicops.events;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.events.event.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class EventDomainTest {
  private EventRecord event() {
    Instant s = Instant.now().plusSeconds(3600);
    return new EventRecord(
        mock(Organization.class),
        mock(User.class),
        "Cleanup",
        null,
        EventType.COMMUNITY_OUTREACH,
        null,
        s,
        s.plusSeconds(3600),
        10,
        true,
        s.minusSeconds(60),
        true,
        null,
        null);
  }

  @Test
  void supportsPublishedRegistrationLifecycle() {
    EventRecord e = event();
    e.transition(EventStatus.PUBLISHED);
    e.transition(EventStatus.REGISTRATION_OPEN);
    e.transition(EventStatus.REGISTRATION_CLOSED);
    e.transition(EventStatus.COMPLETED);
    assertThat(e.getStatus()).isEqualTo(EventStatus.COMPLETED);
  }

  @Test
  void draftCanCancel() {
    EventRecord e = event();
    e.transition(EventStatus.CANCELLED);
    assertThat(e.getStatus()).isEqualTo(EventStatus.CANCELLED);
  }

  @Test
  void publishedCanCancel() {
    EventRecord e = event();
    e.transition(EventStatus.PUBLISHED);
    e.transition(EventStatus.CANCELLED);
    assertThat(e.getStatus()).isEqualTo(EventStatus.CANCELLED);
  }

  @Test
  void completedCannotReopen() {
    EventRecord e = event();
    e.transition(EventStatus.PUBLISHED);
    e.transition(EventStatus.REGISTRATION_OPEN);
    e.transition(EventStatus.REGISTRATION_CLOSED);
    e.transition(EventStatus.COMPLETED);
    assertThatThrownBy(() -> e.transition(EventStatus.REGISTRATION_OPEN))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void terminalEventCannotBePatched() {
    EventRecord e = event();
    e.transition(EventStatus.CANCELLED);
    assertThatThrownBy(
            () -> e.update("x", null, null, null, null, null, null, null, null, null, null, null))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void registrationDeadlineIsApplied() {
    EventRecord e = event();
    e.transition(EventStatus.PUBLISHED);
    e.transition(EventStatus.REGISTRATION_OPEN);
    assertThat(e.acceptsRegistrations(Instant.now())).isTrue();
    assertThat(e.acceptsRegistrations(e.getRegistrationDeadline().plusSeconds(1))).isFalse();
  }
}
