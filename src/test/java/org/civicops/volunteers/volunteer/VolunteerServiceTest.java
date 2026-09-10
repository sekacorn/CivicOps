package org.civicops.volunteers.volunteer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.*;
import org.civicops.core.membership.OrganizationMembershipRepository;
import org.civicops.core.organization.*;
import org.civicops.core.user.UserService;
import org.civicops.shared.exception.ConflictException;
import org.civicops.volunteers.volunteer.dto.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

class VolunteerServiceTest {
  @Mock VolunteerRepository repo;
  @Mock OrganizationService organizations;
  @Mock UserService users;
  @Mock OrganizationMembershipRepository memberships;
  private AutoCloseable mocks;
  private VolunteerService service;
  private UUID orgId;

  @BeforeEach
  void setup() {
    mocks = MockitoAnnotations.openMocks(this);
    service = new VolunteerService(repo, organizations, users, memberships);
    orgId = UUID.randomUUID();
    Organization org = mock(Organization.class);
    when(org.getId()).thenReturn(orgId);
    when(organizations.requireEntity(orgId)).thenReturn(org);
    when(repo.save(any()))
        .thenAnswer(
            i -> {
              Volunteer v = i.getArgument(0);
              ReflectionTestUtils.setField(v, "id", UUID.randomUUID());
              return v;
            });
  }

  @AfterEach
  void close() throws Exception {
    mocks.close();
  }

  @Test
  void createsOrganizationScopedVolunteerWithNormalizedEmailAndSkills() {
    VolunteerDetailResponse response = service.create(orgId, request(" ADA@Example.ORG "));
    assertThat(response.organizationId()).isEqualTo(orgId);
    assertThat(response.email()).isEqualTo("ada@example.org");
    assertThat(response.skills()).containsExactly("first aid");
    assertThat(response.status()).isEqualTo(VolunteerStatus.APPLICANT);
  }

  @Test
  void rejectsDuplicateOrganizationEmail() {
    when(repo.existsByOrganizationIdAndEmail(orgId, "ada@example.org")).thenReturn(true);
    assertThatThrownBy(() -> service.create(orgId, request("ada@example.org")))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void patchNormalizesEmailAndReplacesNormalizedSkills() {
    Volunteer existing =
        new Volunteer(
            organizations.requireEntity(orgId),
            null,
            "Ada",
            "Lovelace",
            "old@example.org",
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
            LocalDate.of(2026, 1, 1),
            Set.of("driving"));
    UUID id = UUID.randomUUID();
    ReflectionTestUtils.setField(existing, "id", id);
    when(repo.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(existing));
    var update =
        new UpdateVolunteerRequest(
            null,
            " Byron ",
            " NEW@Example.ORG ",
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
            Set.of(" Java ", "java"));
    var response = service.update(orgId, id, update);
    assertThat(response.lastName()).isEqualTo("Byron");
    assertThat(response.email()).isEqualTo("new@example.org");
    assertThat(response.skills()).containsExactly("java");
  }

  @Test
  void patchRejectsDuplicateEmail() {
    Volunteer existing =
        new Volunteer(
            organizations.requireEntity(orgId),
            null,
            "Ada",
            "L",
            "old@example.org",
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
    UUID id = UUID.randomUUID();
    ReflectionTestUtils.setField(existing, "id", id);
    when(repo.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(existing));
    when(repo.existsByOrganizationIdAndEmailAndIdNot(orgId, "taken@example.org", id))
        .thenReturn(true);
    var update =
        new UpdateVolunteerRequest(
            null,
            null,
            "TAKEN@example.org",
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
            null);
    assertThatThrownBy(() -> service.update(orgId, id, update))
        .isInstanceOf(ConflictException.class);
  }

  private CreateVolunteerRequest request(String email) {
    return new CreateVolunteerRequest(
        null,
        "Ada",
        "Lovelace",
        email,
        null,
        null,
        null,
        null,
        null,
        null,
        "us",
        null,
        null,
        null,
        null,
        LocalDate.of(2026, 1, 1),
        Set.of(" First Aid ", "first aid"));
  }
}
