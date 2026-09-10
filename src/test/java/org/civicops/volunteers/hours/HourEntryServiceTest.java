package org.civicops.volunteers.hours;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.civicops.volunteers.assignment.*;
import org.civicops.volunteers.hours.dto.CreateHourEntryRequest;
import org.civicops.volunteers.volunteer.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

class HourEntryServiceTest {
  @Mock VolunteerHourEntryRepository hours;
  @Mock VolunteerRepository volunteers;
  @Mock VolunteerAssignmentRepository assignments;
  @Mock UserService users;
  private AutoCloseable mocks;
  private HourEntryService service;

  @BeforeEach
  void setup() {
    mocks = MockitoAnnotations.openMocks(this);
    service =
        new HourEntryService(
            hours,
            volunteers,
            assignments,
            users,
            Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC));
  }

  @AfterEach
  void close() throws Exception {
    mocks.close();
  }

  @Test
  void volunteerCannotApproveOwnHours() {
    UUID orgId = UUID.randomUUID(),
        volunteerId = UUID.randomUUID(),
        userId = UUID.randomUUID(),
        hourId = UUID.randomUUID();
    User linkedUser = mock(User.class);
    when(linkedUser.getId()).thenReturn(userId);
    Volunteer volunteer = volunteer(volunteerId, linkedUser);
    VolunteerHourEntry entry =
        new VolunteerHourEntry(
            null, volunteer, null, LocalDate.now(), new BigDecimal("2.00"), null);
    when(hours.findByIdAndOrganizationId(hourId, orgId)).thenReturn(Optional.of(entry));
    assertThatThrownBy(() -> service.approve(orgId, hourId, userId))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("own hours");
  }

  @Test
  void linkedAssignmentMustBelongToSubmittedVolunteer() {
    UUID orgId = UUID.randomUUID(),
        volunteerId = UUID.randomUUID(),
        otherId = UUID.randomUUID(),
        assignmentId = UUID.randomUUID();
    Volunteer volunteer = volunteer(volunteerId, null);
    Volunteer other = volunteer(otherId, null);
    VolunteerAssignment assignment = new VolunteerAssignment(null, other, null);
    when(volunteers.findByIdAndOrganizationId(volunteerId, orgId))
        .thenReturn(Optional.of(volunteer));
    when(assignments.findByIdAndOrganizationId(assignmentId, orgId))
        .thenReturn(Optional.of(assignment));
    var request =
        new CreateHourEntryRequest(
            volunteerId, assignmentId, LocalDate.now(), new BigDecimal("1.00"), null);
    assertThatThrownBy(() -> service.submit(orgId, request))
        .isInstanceOf(BusinessRuleException.class);
  }

  private Volunteer volunteer(UUID id, User user) {
    Volunteer volunteer =
        new Volunteer(
            mock(Organization.class),
            user,
            "Test",
            "Volunteer",
            id + "@example.org",
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
    ReflectionTestUtils.setField(volunteer, "id", id);
    return volunteer;
  }
}
