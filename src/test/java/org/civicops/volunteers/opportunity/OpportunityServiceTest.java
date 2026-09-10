package org.civicops.volunteers.opportunity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.UUID;
import org.civicops.core.organization.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.civicops.volunteers.opportunity.dto.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

class OpportunityServiceTest {
  @Mock VolunteerOpportunityRepository repo;
  @Mock OrganizationService orgs;
  @Mock org.civicops.volunteers.shift.VolunteerShiftRepository shifts;
  private AutoCloseable mocks;
  private OpportunityService service;

  @BeforeEach
  void setup() {
    mocks = MockitoAnnotations.openMocks(this);
    service = new OpportunityService(repo, orgs, shifts);
  }

  @AfterEach
  void close() throws Exception {
    mocks.close();
  }

  @Test
  void rejectsEndBeforeStart() {
    Instant start = Instant.now().plusSeconds(7200);
    var r =
        new CreateOpportunityRequest("Food drive", null, null, start, start.minusSeconds(1), 1, 5);
    assertThatThrownBy(() -> service.create(UUID.randomUUID(), r))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void rejectsMinimumAboveMaximum() {
    Instant start = Instant.now().plusSeconds(7200);
    var r =
        new CreateOpportunityRequest(
            "Food drive", null, null, start, start.plusSeconds(3600), 6, 5);
    assertThatThrownBy(() -> service.create(UUID.randomUUID(), r))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void completedOpportunityCannotReopen() {
    VolunteerOpportunity o =
        new VolunteerOpportunity(
            null,
            "Food drive",
            null,
            null,
            Instant.now().plusSeconds(3600),
            Instant.now().plusSeconds(7200),
            1,
            5);
    o.open();
    o.close();
    o.complete();
    assertThatThrownBy(o::open).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void updateRejectsWindowThatInvalidatesShift() {
    UUID orgId = UUID.randomUUID(), id = UUID.randomUUID();
    Instant start = Instant.now().plusSeconds(7200);
    VolunteerOpportunity opportunity =
        new VolunteerOpportunity(null, "Event", null, null, start, start.plusSeconds(7200), 1, 5);
    when(repo.findByIdAndOrganizationId(id, orgId)).thenReturn(java.util.Optional.of(opportunity));
    when(shifts.countOutsideWindow(eq(id), any(), any())).thenReturn(1L);
    var request =
        new UpdateOpportunityRequest(null, null, null, start.plusSeconds(100), null, null, null);
    assertThatThrownBy(() -> service.update(orgId, id, request))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("existing shifts");
  }

  @Test
  void terminalOpportunityCannotBeEdited() {
    UUID orgId = UUID.randomUUID(), id = UUID.randomUUID();
    Instant start = Instant.now().plusSeconds(7200);
    VolunteerOpportunity opportunity =
        new VolunteerOpportunity(null, "Event", null, null, start, start.plusSeconds(7200), 1, 5);
    opportunity.cancel();
    when(repo.findByIdAndOrganizationId(id, orgId)).thenReturn(java.util.Optional.of(opportunity));
    assertThatThrownBy(
            () ->
                service.update(
                    orgId,
                    id,
                    new UpdateOpportunityRequest("Changed", null, null, null, null, null, null)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void updatesValidOpportunityFields() {
    UUID orgId = UUID.randomUUID(), id = UUID.randomUUID();
    Instant start = Instant.now().plusSeconds(7200);
    org.civicops.core.organization.Organization organization =
        mock(org.civicops.core.organization.Organization.class);
    when(organization.getId()).thenReturn(orgId);
    VolunteerOpportunity opportunity =
        new VolunteerOpportunity(
            organization, "Old", null, null, start, start.plusSeconds(7200), 1, 5);
    when(repo.findByIdAndOrganizationId(id, orgId)).thenReturn(java.util.Optional.of(opportunity));
    var response =
        service.update(
            orgId, id, new UpdateOpportunityRequest("New", null, "Hall", null, null, 2, 6));
    assertThat(response.title()).isEqualTo("New");
    assertThat(response.maximumVolunteers()).isEqualTo(6);
  }
}
