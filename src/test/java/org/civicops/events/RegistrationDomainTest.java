package org.civicops.events;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import org.civicops.core.organization.Organization;
import org.civicops.events.event.*;
import org.civicops.events.registration.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class RegistrationDomainTest {
  private EventRegistration registration(RegistrationStatus status) {
    Instant s = Instant.now().plusSeconds(3600);
    EventRecord e =
        new EventRecord(
            mock(Organization.class),
            null,
            "Event",
            null,
            EventType.OTHER,
            null,
            s,
            s.plusSeconds(10),
            1,
            true,
            null,
            true,
            null,
            null);
    return new EventRegistration(
        mock(Organization.class),
        e,
        null,
        "External",
        "person@example.org",
        null,
        status,
        Instant.now());
  }

  @Test
  void registeredAttendeeCanCheckInAndOut() {
    EventRegistration r = registration(RegistrationStatus.REGISTERED);
    r.checkIn(Instant.now());
    r.checkOut(Instant.now());
    assertThat(r.getStatus()).isEqualTo(RegistrationStatus.ATTENDED);
    assertThat(r.getCheckedOutAt()).isNotNull();
  }

  @Test
  void waitlistedCannotCheckIn() {
    assertThatThrownBy(() -> registration(RegistrationStatus.WAITLISTED).checkIn(Instant.now()))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void cancelledCannotCheckIn() {
    assertThatThrownBy(() -> registration(RegistrationStatus.CANCELLED).checkIn(Instant.now()))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void checkoutRequiresCheckin() {
    assertThatThrownBy(() -> registration(RegistrationStatus.REGISTERED).checkOut(Instant.now()))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void duplicateCheckinIsRejected() {
    EventRegistration r = registration(RegistrationStatus.REGISTERED);
    r.checkIn(Instant.now());
    assertThatThrownBy(() -> r.checkIn(Instant.now())).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void duplicateCheckoutIsRejected() {
    EventRegistration r = registration(RegistrationStatus.REGISTERED);
    r.checkIn(Instant.now());
    r.checkOut(Instant.now());
    assertThatThrownBy(() -> r.checkOut(Instant.now())).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void cancellationPromotesOnlyWaitlisted() {
    EventRegistration r = registration(RegistrationStatus.WAITLISTED);
    r.promote();
    assertThat(r.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
  }

  @Test
  void cancellationCannotRepeat() {
    EventRegistration r = registration(RegistrationStatus.REGISTERED);
    r.cancel();
    assertThatThrownBy(r::cancel).isInstanceOf(BusinessRuleException.class);
  }
}
