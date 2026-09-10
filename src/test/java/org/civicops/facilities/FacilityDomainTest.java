package org.civicops.facilities;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.facilities.facility.*;
import org.civicops.facilities.reservation.*;
import org.civicops.facilities.space.FacilitySpace;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class FacilityDomainTest {
  private final Organization org = mock(Organization.class);
  private final User user = mock(User.class);

  private FacilitySpace space() {
    Facility f =
        new Facility(
            org,
            "Center",
            null,
            FacilityType.COMMUNITY_CENTER,
            null,
            null,
            null,
            null,
            null,
            null,
            "America/New_York",
            null);
    return new FacilitySpace(f, "Room", "room", null, 30, true, null, null);
  }

  private FacilityReservation reservation(Instant start, Instant end) {
    return new FacilityReservation(
        space(),
        user,
        null,
        null,
        null,
        "Meeting",
        null,
        start,
        end,
        20,
        start.minusSeconds(60),
        null);
  }

  @Test
  void reservationLifecycleApprovesAndCompletesAfterUse() {
    Instant a = Instant.parse("2030-01-01T15:00:00Z"), b = a.plusSeconds(3600);
    var r = reservation(a, b);
    r.approve(user, a.minusSeconds(10));
    r.complete(b);
    assertThat(r.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
  }

  @Test
  void rejectionPreservesReason() {
    var r = reservation(Instant.now().plusSeconds(100), Instant.now().plusSeconds(200));
    r.reject(user, Instant.now(), "Conflict");
    assertThat(r.getStatus()).isEqualTo(ReservationStatus.REJECTED);
    assertThat(r.getRejectionReason()).isEqualTo("Conflict");
  }

  @Test
  void pendingAndApprovedCanCancelButTerminalCannot() {
    var r = reservation(Instant.now().plusSeconds(100), Instant.now().plusSeconds(200));
    r.cancel(Instant.now(), "Changed");
    assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    assertThatThrownBy(() -> r.cancel(Instant.now(), null))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void futureApprovedReservationCannotComplete() {
    Instant a = Instant.now().plusSeconds(100);
    var r = reservation(a, a.plusSeconds(100));
    r.approve(user, Instant.now());
    assertThatThrownBy(() -> r.complete(Instant.now())).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void onlyPendingReservationCanApproveOrReject() {
    Instant a = Instant.now().plusSeconds(100);
    var r = reservation(a, a.plusSeconds(100));
    r.reject(user, Instant.now(), "No");
    assertThatThrownBy(() -> r.approve(user, Instant.now()))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void facilitySpaceSeparatesActiveAndReservable() {
    FacilitySpace s = space();
    s.update(null, null, null, null, false, false, null, null);
    assertThat(s.isActive()).isFalse();
    assertThat(s.isReservable()).isFalse();
  }
}
