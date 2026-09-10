package org.civicops.donations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.civicops.core.organization.*;
import org.civicops.core.user.*;
import org.civicops.donations.campaign.*;
import org.civicops.donations.donation.*;
import org.civicops.donations.donation.dto.*;
import org.civicops.donations.donor.*;
import org.civicops.shared.exception.*;
import org.junit.jupiter.api.*;

class DonationServiceTest {
  DonationRepository donations = mock(DonationRepository.class);
  DonorRepository donors = mock(DonorRepository.class);
  DonationCampaignRepository campaigns = mock(DonationCampaignRepository.class);
  OrganizationService orgs = mock(OrganizationService.class);
  UserService users = mock(UserService.class);
  DonationService service = new DonationService(donations, donors, campaigns, orgs, users);
  UUID org = UUID.randomUUID(),
      user = UUID.randomUUID(),
      donorId = UUID.randomUUID(),
      campaignId = UUID.randomUUID();
  Organization organization = mock(Organization.class);
  User creator = mock(User.class);

  @BeforeEach
  void setup() {
    when(organization.getId()).thenReturn(org);
    when(creator.getId()).thenReturn(user);
    when(orgs.requireEntity(org)).thenReturn(organization);
    when(users.requireEntity(user)).thenReturn(creator);
    when(donations.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  void recordsIdentifiedDonation() {
    identifiedDonor();
    var result =
        service.create(
            org, user, request(donorId, false, null, DonationPaymentMethod.ACH, "100.25", false));
    assertThat(result.amount()).isEqualByComparingTo("100.25");
    assertThat(result.acknowledgementStatus()).isEqualTo(AcknowledgementStatus.PENDING);
  }

  @Test
  void recordsAnonymousDonationWithoutDonor() {
    var result =
        service.create(
            org, user, request(null, true, null, DonationPaymentMethod.CHECK, "3750", false));
    assertThat(result.anonymous()).isTrue();
    assertThat(result.donorId()).isNull();
    assertThat(result.acknowledgementStatus()).isEqualTo(AcknowledgementStatus.NOT_REQUIRED);
  }

  @Test
  void nonAnonymousDonationRequiresDonor() {
    assertThatThrownBy(
            () ->
                service.create(
                    org, user, request(null, false, null, DonationPaymentMethod.CASH, "10", false)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void anonymousDonationRejectsRetainedDonor() {
    assertThatThrownBy(
            () ->
                service.create(
                    org,
                    user,
                    request(donorId, true, null, DonationPaymentMethod.CASH, "10", false)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void crossOrganizationDonorIsRejected() {
    when(donors.findByIdAndOrganizationId(donorId, org)).thenReturn(Optional.empty());
    assertThatThrownBy(
            () ->
                service.create(
                    org,
                    user,
                    request(donorId, false, null, DonationPaymentMethod.CASH, "10", false)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void activeCampaignAcceptsDonation() {
    identifiedDonor();
    DonationCampaign c = mock(DonationCampaign.class);
    when(c.getStatus()).thenReturn(CampaignStatus.ACTIVE);
    when(c.getId()).thenReturn(campaignId);
    when(campaigns.findByIdAndOrganizationId(campaignId, org)).thenReturn(Optional.of(c));
    assertThatCode(
            () ->
                service.create(
                    org,
                    user,
                    request(donorId, false, campaignId, DonationPaymentMethod.CARD, "10", false)))
        .doesNotThrowAnyException();
  }

  @Test
  void terminalCampaignRejectsDonation() {
    identifiedDonor();
    DonationCampaign c = mock(DonationCampaign.class);
    when(c.getStatus()).thenReturn(CampaignStatus.CLOSED);
    when(campaigns.findByIdAndOrganizationId(campaignId, org)).thenReturn(Optional.of(c));
    assertThatThrownBy(
            () ->
                service.create(
                    org,
                    user,
                    request(donorId, false, campaignId, DonationPaymentMethod.CARD, "10", false)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void restrictedDonationRequiresPurpose() {
    identifiedDonor();
    var r =
        new CreateDonationRequest(
            donorId,
            false,
            BigDecimal.TEN,
            LocalDate.now(),
            DonationPaymentMethod.ACH,
            null,
            null,
            true,
            null,
            null,
            null,
            null,
            null,
            null);
    assertThatThrownBy(() -> service.create(org, user, r))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void inKindDonationRequiresDescription() {
    identifiedDonor();
    assertThatThrownBy(
            () ->
                service.create(
                    org,
                    user,
                    request(donorId, false, null, DonationPaymentMethod.IN_KIND, "10", false)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void duplicateReferenceIsRejected() {
    identifiedDonor();
    when(donations.existsByOrganizationIdAndReferenceNumber(org, "REF-1")).thenReturn(true);
    var r =
        new CreateDonationRequest(
            donorId,
            false,
            BigDecimal.TEN,
            LocalDate.now(),
            DonationPaymentMethod.ACH,
            null,
            null,
            false,
            null,
            null,
            "REF-1",
            null,
            null,
            null);
    assertThatThrownBy(() -> service.create(org, user, r)).isInstanceOf(ConflictException.class);
  }

  private void identifiedDonor() {
    Donor d = mock(Donor.class);
    when(d.getId()).thenReturn(donorId);
    when(donors.findByIdAndOrganizationId(donorId, org)).thenReturn(Optional.of(d));
  }

  private CreateDonationRequest request(
      UUID donor,
      boolean anonymous,
      UUID campaign,
      DonationPaymentMethod method,
      String amount,
      boolean restricted) {
    return new CreateDonationRequest(
        donor,
        anonymous,
        new BigDecimal(amount),
        LocalDate.now(),
        method,
        null,
        campaign,
        restricted,
        null,
        restricted ? "Youth program" : null,
        null,
        null,
        null,
        null);
  }
}
