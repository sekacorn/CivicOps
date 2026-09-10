package org.civicops.donations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.donations.campaign.*;
import org.civicops.donations.donation.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class DonationDomainTest {
  @Test
  void campaignActivatesAndCloses() {
    DonationCampaign c = campaign();
    c.transition(CampaignStatus.ACTIVE);
    c.transition(CampaignStatus.CLOSED);
    assertThat(c.getStatus()).isEqualTo(CampaignStatus.CLOSED);
  }

  @Test
  void draftCampaignCanCancel() {
    DonationCampaign c = campaign();
    c.transition(CampaignStatus.CANCELLED);
    assertThat(c.getStatus()).isEqualTo(CampaignStatus.CANCELLED);
  }

  @Test
  void activeCampaignCanCancel() {
    DonationCampaign c = campaign();
    c.transition(CampaignStatus.ACTIVE);
    c.transition(CampaignStatus.CANCELLED);
    assertThat(c.getStatus()).isEqualTo(CampaignStatus.CANCELLED);
  }

  @Test
  void closedCampaignCannotReactivate() {
    DonationCampaign c = campaign();
    c.transition(CampaignStatus.ACTIVE);
    c.transition(CampaignStatus.CLOSED);
    assertThatThrownBy(() -> c.transition(CampaignStatus.ACTIVE))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void cancelledCampaignCannotReactivate() {
    DonationCampaign c = campaign();
    c.transition(CampaignStatus.CANCELLED);
    assertThatThrownBy(() -> c.transition(CampaignStatus.ACTIVE))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void terminalCampaignCannotBePatched() {
    DonationCampaign c = campaign();
    c.transition(CampaignStatus.CANCELLED);
    assertThatThrownBy(() -> c.update("x", null, null, null, null))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void donationCanBeReversedOnlyOnce() {
    Donation d = donation();
    User user = mock(User.class);
    d.reverse(user, "Duplicate entry");
    assertThat(d.getStatus()).isEqualTo(DonationStatus.REVERSED);
    assertThat(d.getReversedAt()).isNotNull();
    assertThatThrownBy(() -> d.reverse(user, "Again")).isInstanceOf(BusinessRuleException.class);
  }

  private DonationCampaign campaign() {
    return new DonationCampaign(
        mock(Organization.class),
        mock(User.class),
        "Drive",
        null,
        new BigDecimal("25000"),
        LocalDate.now(),
        LocalDate.now().plusDays(30));
  }

  private Donation donation() {
    return new Donation(
        mock(Organization.class),
        null,
        null,
        true,
        new BigDecimal("10"),
        LocalDate.now(),
        DonationPaymentMethod.CASH,
        null,
        false,
        null,
        null,
        null,
        null,
        AcknowledgementStatus.NOT_REQUIRED,
        null,
        mock(User.class));
  }
}
