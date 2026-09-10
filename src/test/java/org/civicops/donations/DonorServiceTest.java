package org.civicops.donations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.civicops.core.organization.*;
import org.civicops.donations.donor.*;
import org.civicops.donations.donor.dto.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.*;

class DonorServiceTest {
  DonorRepository donors = mock(DonorRepository.class);
  OrganizationService orgs = mock(OrganizationService.class);
  DonorService service = new DonorService(donors, orgs);
  UUID org = UUID.randomUUID();

  @BeforeEach
  void setup() {
    Organization o = mock(Organization.class);
    when(o.getId()).thenReturn(org);
    when(orgs.requireEntity(org)).thenReturn(o);
    when(donors.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  void createsIndividualAndNormalizesEmail() {
    var r = create(DonorType.INDIVIDUAL, " Ada ", " Lovelace ", null, " ADA@EXAMPLE.ORG ", false);
    var d = service.create(org, r);
    assertThat(d.firstName()).isEqualTo("Ada");
    assertThat(d.email()).isEqualTo("ada@example.org");
  }

  @Test
  void createsOrganizationDonor() {
    assertThatCode(
            () ->
                service.create(
                    org, create(DonorType.FOUNDATION, null, null, "Foundation", null, false)))
        .doesNotThrowAnyException();
  }

  @Test
  void supportsAnonymousDonorProfileWithoutFakeName() {
    var d = service.create(org, create(DonorType.OTHER, null, null, null, null, true));
    assertThat(d.anonymous()).isTrue();
    assertThat(d.firstName()).isNull();
  }

  @Test
  void individualRequiresRealNames() {
    assertThatThrownBy(
            () -> service.create(org, create(DonorType.INDIVIDUAL, "Ada", null, null, null, false)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void organizationRequiresOrganizationName() {
    assertThatThrownBy(
            () ->
                service.create(org, create(DonorType.ORGANIZATION, null, null, null, null, false)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void patchPreservesOmittedFieldsAndNormalizesEmail() {
    Donor donor =
        new Donor(
            orgs.requireEntity(org),
            DonorType.INDIVIDUAL,
            "Ada",
            "Lovelace",
            null,
            "old@example.org",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            false,
            null);
    when(donors.findByIdAndOrganizationId(any(), eq(org))).thenReturn(Optional.of(donor));
    var result =
        service.update(
            org,
            UUID.randomUUID(),
            new UpdateDonorRequest(
                null,
                null,
                null,
                " NEW@EXAMPLE.ORG ",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null));
    assertThat(result.firstName()).isEqualTo("Ada");
    assertThat(result.email()).isEqualTo("new@example.org");
    assertThat(result.communicationOptOut()).isTrue();
  }

  private CreateDonorRequest create(
      DonorType type, String first, String last, String name, String email, boolean anonymous) {
    return new CreateDonorRequest(
        type, first, last, name, email, null, null, null, null, null, null, null, anonymous, false,
        null);
  }
}
