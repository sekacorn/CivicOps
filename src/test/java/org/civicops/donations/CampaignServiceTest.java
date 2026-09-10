package org.civicops.donations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.civicops.core.organization.*;
import org.civicops.core.user.*;
import org.civicops.donations.campaign.*;
import org.civicops.donations.campaign.dto.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.*;

class CampaignServiceTest {
  DonationCampaignRepository repo = mock(DonationCampaignRepository.class);
  OrganizationService orgs = mock(OrganizationService.class);
  UserService users = mock(UserService.class);
  DonationCampaignService service = new DonationCampaignService(repo, orgs, users);
  UUID org = UUID.randomUUID(), user = UUID.randomUUID();

  @BeforeEach
  void setup() {
    Organization o = mock(Organization.class);
    User u = mock(User.class);
    when(o.getId()).thenReturn(org);
    when(u.getId()).thenReturn(user);
    when(orgs.requireEntity(org)).thenReturn(o);
    when(users.requireEntity(user)).thenReturn(u);
    when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  void createsCampaignWithExactGoal() {
    var r =
        new CreateCampaignRequest(
            "Food Drive",
            null,
            new BigDecimal("25000"),
            LocalDate.now(),
            LocalDate.now().plusDays(30));
    assertThat(service.create(org, user, r).goalAmount()).isEqualByComparingTo("25000.00");
  }

  @Test
  void rejectsInvalidDates() {
    var r =
        new CreateCampaignRequest(
            "Drive", null, BigDecimal.TEN, LocalDate.now(), LocalDate.now().minusDays(1));
    assertThatThrownBy(() -> service.create(org, user, r))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void validPatchPreservesOmittedName() {
    DonationCampaign c = campaign();
    when(repo.findByIdAndOrganizationId(any(), eq(org))).thenReturn(Optional.of(c));
    var x =
        service.update(
            org,
            UUID.randomUUID(),
            new UpdateCampaignRequest(null, "Updated", new BigDecimal("30000"), null, null));
    assertThat(x.name()).isEqualTo("Drive");
    assertThat(x.goalAmount()).isEqualByComparingTo("30000");
  }

  @Test
  void terminalPatchRejected() {
    DonationCampaign c = campaign();
    c.transition(CampaignStatus.CANCELLED);
    when(repo.findByIdAndOrganizationId(any(), eq(org))).thenReturn(Optional.of(c));
    assertThatThrownBy(
            () ->
                service.update(
                    org, UUID.randomUUID(), new UpdateCampaignRequest("x", null, null, null, null)))
        .isInstanceOf(BusinessRuleException.class);
  }

  private DonationCampaign campaign() {
    return new DonationCampaign(
        orgs.requireEntity(org),
        users.requireEntity(user),
        "Drive",
        null,
        new BigDecimal("25000"),
        LocalDate.now(),
        LocalDate.now().plusDays(30));
  }
}
