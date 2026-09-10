package org.civicops.volunteers.shift;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.UUID;
import org.civicops.shared.exception.BusinessRuleException;
import org.civicops.volunteers.assignment.AssignmentStatus;
import org.civicops.volunteers.opportunity.*;
import org.civicops.volunteers.shift.dto.CreateShiftRequest;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

class ShiftServiceTest {
  @Mock VolunteerShiftRepository repo;
  @Mock OpportunityService opportunities;
  @Mock org.civicops.volunteers.assignment.VolunteerAssignmentRepository assignments;
  private AutoCloseable mocks;
  private ShiftService service;

  @BeforeEach
  void setup() {
    mocks = MockitoAnnotations.openMocks(this);
    service = new ShiftService(repo, opportunities, assignments);
  }

  @AfterEach
  void close() throws Exception {
    mocks.close();
  }

  @Test
  void rejectsShiftOutsideOpportunityWindow() {
    UUID org = UUID.randomUUID(), opp = UUID.randomUUID();
    Instant start = Instant.now().plusSeconds(7200);
    VolunteerOpportunity o =
        new VolunteerOpportunity(null, "Event", null, null, start, start.plusSeconds(7200), 1, 10);
    when(opportunities.require(org, opp)).thenReturn(o);
    var r = new CreateShiftRequest("Early", start.minusSeconds(1), start.plusSeconds(1000), 2);
    assertThatThrownBy(() -> service.create(org, opp, r)).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void cancelledOpportunityRejectsNewShift() {
    UUID org = UUID.randomUUID(), opp = UUID.randomUUID();
    Instant start = Instant.now().plusSeconds(7200);
    VolunteerOpportunity o =
        new VolunteerOpportunity(null, "Event", null, null, start, start.plusSeconds(7200), 1, 10);
    o.cancel();
    when(opportunities.require(org, opp)).thenReturn(o);
    assertThatThrownBy(
            () ->
                service.create(
                    org, opp, new CreateShiftRequest("Shift", start, start.plusSeconds(1000), 2)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void updateRejectsCapacityBelowActiveAssignments() {
    UUID org = UUID.randomUUID(), opp = UUID.randomUUID(), shiftId = UUID.randomUUID();
    Instant start = Instant.now().plusSeconds(7200);
    VolunteerOpportunity opportunity =
        new VolunteerOpportunity(null, "Event", null, null, start, start.plusSeconds(7200), 1, 10);
    VolunteerShift shift =
        new VolunteerShift(null, opportunity, "Shift", start, start.plusSeconds(1000), 10);
    ReflectionTestUtils.setField(opportunity, "id", opp);
    ReflectionTestUtils.setField(shift, "id", shiftId);
    when(opportunities.require(org, opp)).thenReturn(opportunity);
    when(repo.findByIdAndOrganizationId(shiftId, org)).thenReturn(java.util.Optional.of(shift));
    when(assignments.countByShiftIdAndStatusNot(shiftId, AssignmentStatus.CANCELLED))
        .thenReturn(7L);
    assertThatThrownBy(
            () ->
                service.update(
                    org,
                    opp,
                    shiftId,
                    new org.civicops.volunteers.shift.dto.UpdateShiftRequest(null, null, null, 6)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("active assignments");
  }

  @Test
  void updatesValidShiftCapacity() {
    UUID org = UUID.randomUUID(), opp = UUID.randomUUID(), shiftId = UUID.randomUUID();
    Instant start = Instant.now().plusSeconds(7200);
    VolunteerOpportunity opportunity =
        new VolunteerOpportunity(null, "Event", null, null, start, start.plusSeconds(7200), 1, 10);
    VolunteerShift shift =
        new VolunteerShift(null, opportunity, "Shift", start, start.plusSeconds(1000), 10);
    ReflectionTestUtils.setField(opportunity, "id", opp);
    ReflectionTestUtils.setField(shift, "id", shiftId);
    when(opportunities.require(org, opp)).thenReturn(opportunity);
    when(repo.findByIdAndOrganizationId(shiftId, org)).thenReturn(java.util.Optional.of(shift));
    when(assignments.countByShiftIdAndStatusNot(shiftId, AssignmentStatus.CANCELLED))
        .thenReturn(7L);
    assertThat(
            service
                .update(
                    org,
                    opp,
                    shiftId,
                    new org.civicops.volunteers.shift.dto.UpdateShiftRequest(
                        "Updated", null, null, 8))
                .capacity())
        .isEqualTo(8);
  }
}
