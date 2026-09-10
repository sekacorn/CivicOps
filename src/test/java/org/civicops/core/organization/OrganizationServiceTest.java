package org.civicops.core.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.civicops.core.organization.dto.CreateOrganizationRequest;
import org.civicops.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {
  @Mock OrganizationRepository organizations;

  @Test
  void createsAndNormalizesOrganization() {
    when(organizations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    OrganizationService service = new OrganizationService(organizations);

    var result =
        service.create(
            new CreateOrganizationRequest(
                "  Hope Center  ",
                " Hope Center Inc. ",
                null,
                OrganizationType.NONPROFIT,
                " INFO@HOPE.ORG ",
                null,
                null,
                null,
                null,
                " Baltimore ",
                " MD ",
                "21201",
                "us"));

    assertThat(result.name()).isEqualTo("Hope Center");
    assertThat(result.email()).isEqualTo("info@hope.org");
    assertThat(result.country()).isEqualTo("US");
    assertThat(result.active()).isTrue();
  }

  @Test
  void looksUpOrganization() {
    UUID id = UUID.randomUUID();
    Organization organization =
        new Organization(
            "Hope Center",
            null,
            null,
            OrganizationType.NONPROFIT,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    when(organizations.findById(id)).thenReturn(Optional.of(organization));

    assertThat(new OrganizationService(organizations).get(id).name()).isEqualTo("Hope Center");
  }

  @Test
  void rejectsUnknownOrganization() {
    UUID id = UUID.randomUUID();
    when(organizations.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> new OrganizationService(organizations).get(id))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Organization");
  }
}
