package org.civicops.volunteers.assignment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.*;
import org.civicops.shared.exception.*;
import org.civicops.volunteers.opportunity.*;
import org.civicops.volunteers.shift.*;
import org.civicops.volunteers.volunteer.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

class AssignmentServiceTest {
  @Mock VolunteerAssignmentRepository assignments;
  @Mock VolunteerShiftRepository shifts;
  @Mock VolunteerRepository volunteers;
  private AutoCloseable mocks;
  private AssignmentService service;
  private final UUID orgId = UUID.randomUUID(),
      shiftId = UUID.randomUUID(),
      volunteerId = UUID.randomUUID();

  @BeforeEach
  void setup() {
    mocks = MockitoAnnotations.openMocks(this);
    service =
        new AssignmentService(
            assignments,
            shifts,
            volunteers,
            Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC));
  }

  @AfterEach
  void close() throws Exception {
    mocks.close();
  }

  @Test
  void registersEligibleVolunteerAgainstLockedShift() {
    VolunteerShift shift = openShift();
    Volunteer volunteer = activeVolunteer();
    when(shifts.findForRegistration(shiftId, orgId)).thenReturn(Optional.of(shift));
    when(volunteers.findByIdAndOrganizationId(volunteerId, orgId))
        .thenReturn(Optional.of(volunteer));
    when(assignments.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              VolunteerAssignment a = i.getArgument(0);
              ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
              return a;
            });
    assertThat(service.register(orgId, shiftId, volunteerId).status())
        .isEqualTo(AssignmentStatus.REGISTERED);
    verify(shifts).findForRegistration(shiftId, orgId);
  }

  @Test
  void rejectsDuplicateActiveAssignment() {
    VolunteerShift shift = openShift();
    Volunteer volunteer = activeVolunteer();
    when(shifts.findForRegistration(shiftId, orgId)).thenReturn(Optional.of(shift));
    when(volunteers.findByIdAndOrganizationId(volunteerId, orgId))
        .thenReturn(Optional.of(volunteer));
    when(assignments.existsByVolunteerIdAndShiftIdAndStatusNot(
            volunteerId, shiftId, AssignmentStatus.CANCELLED))
        .thenReturn(true);
    assertThatThrownBy(() -> service.register(orgId, shiftId, volunteerId))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already assigned");
  }

  @Test
  void rejectsFullShift() {
    VolunteerShift shift = openShift();
    Volunteer volunteer = activeVolunteer();
    when(shifts.findForRegistration(shiftId, orgId)).thenReturn(Optional.of(shift));
    when(volunteers.findByIdAndOrganizationId(volunteerId, orgId))
        .thenReturn(Optional.of(volunteer));
    when(assignments.countByShiftIdAndStatusNot(shiftId, AssignmentStatus.CANCELLED))
        .thenReturn(2L);
    assertThatThrownBy(() -> service.register(orgId, shiftId, volunteerId))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("capacity");
  }

  private VolunteerShift openShift() {
    VolunteerOpportunity o =
        new VolunteerOpportunity(
            null,
            "Cleanup",
            null,
            null,
            Instant.parse("2030-02-01T10:00:00Z"),
            Instant.parse("2030-02-01T18:00:00Z"),
            1,
            10);
    o.open();
    VolunteerShift s =
        new VolunteerShift(
            null,
            o,
            "Morning",
            Instant.parse("2030-02-01T10:00:00Z"),
            Instant.parse("2030-02-01T12:00:00Z"),
            2);
    ReflectionTestUtils.setField(s, "id", shiftId);
    return s;
  }

  private Volunteer activeVolunteer() {
    Volunteer v =
        new Volunteer(
            null,
            null,
            "Ada",
            "Lovelace",
            "ada@example.org",
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
    ReflectionTestUtils.setField(v, "id", volunteerId);
    return v;
  }
}
