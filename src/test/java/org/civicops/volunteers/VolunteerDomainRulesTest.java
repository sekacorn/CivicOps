package org.civicops.volunteers;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.Set;
import org.civicops.shared.exception.BusinessRuleException;
import org.civicops.volunteers.assignment.*;
import org.civicops.volunteers.hours.*;
import org.civicops.volunteers.opportunity.*;
import org.civicops.volunteers.volunteer.*;
import org.junit.jupiter.api.Test;

class VolunteerDomainRulesTest {
  @Test
  void opportunityRejectsInvalidTerminalTransition() {
    VolunteerOpportunity o =
        new VolunteerOpportunity(
            null,
            "Event",
            null,
            null,
            Instant.now().plusSeconds(3600),
            Instant.now().plusSeconds(7200),
            1,
            5);
    o.open();
    o.close();
    o.complete();
    assertThatThrownBy(o::cancel).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void assignmentRequiresCheckInBeforeCheckOut() {
    Volunteer v =
        new Volunteer(
            null,
            null,
            "A",
            "B",
            "a@b.org",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            VolunteerStatus.ACTIVE,
            null,
            Set.of());
    VolunteerAssignment a = new VolunteerAssignment(null, v, null);
    assertThatThrownBy(() -> a.checkOut(Instant.now())).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void reviewedHoursCannotBeProcessedTwice() {
    Volunteer v =
        new Volunteer(
            null,
            null,
            "A",
            "B",
            "a@b.org",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            VolunteerStatus.ACTIVE,
            null,
            Set.of());
    VolunteerHourEntry h =
        new VolunteerHourEntry(null, v, null, LocalDate.now(), new BigDecimal("2.50"), null);
    h.approve(null, Instant.now());
    assertThatThrownBy(() -> h.reject(null, Instant.now(), "late"))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void duplicateCheckInAndCheckOutAreRejected() {
    Volunteer v =
        new Volunteer(
            null,
            null,
            "A",
            "B",
            "a@b.org",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            VolunteerStatus.ACTIVE,
            null,
            Set.of());
    VolunteerAssignment assignment = new VolunteerAssignment(null, v, null);
    Instant now = Instant.now();
    assignment.checkIn(now);
    assertThatThrownBy(() -> assignment.checkIn(now.plusSeconds(1)))
        .isInstanceOf(BusinessRuleException.class);
    assignment.checkOut(now.plusSeconds(2));
    assertThatThrownBy(() -> assignment.checkOut(now.plusSeconds(3)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void cancelledAssignmentCannotCheckIn() {
    Volunteer v =
        new Volunteer(
            null,
            null,
            "A",
            "B",
            "a@b.org",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            VolunteerStatus.ACTIVE,
            null,
            Set.of());
    VolunteerAssignment assignment = new VolunteerAssignment(null, v, null);
    assignment.cancel();
    assertThatThrownBy(() -> assignment.checkIn(Instant.now()))
        .isInstanceOf(BusinessRuleException.class);
  }
}
