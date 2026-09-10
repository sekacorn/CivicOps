package org.civicops.donations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.civicops.donations.campaign.*;
import org.civicops.donations.donation.*;
import org.civicops.donations.donor.Donor;
import org.civicops.donations.donor.DonorRepository;
import org.civicops.donations.reporting.DonationReportingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class DonationReportingServiceTest {
  DonationRepository donations = mock(DonationRepository.class);
  DonationCampaignRepository campaigns = mock(DonationCampaignRepository.class);
  DonorRepository donors = mock(DonorRepository.class);
  DonationReportingService service = new DonationReportingService(donations, campaigns, donors);
  UUID org = UUID.randomUUID(), campaignId = UUID.randomUUID();
  LocalDate min = LocalDate.of(1, 1, 1), max = LocalDate.of(9999, 12, 31);

  @Test
  void campaignCalculatesExactGoalProgress() {
    stubCampaign("25000", "18750", 3);
    var r = service.campaign(org, campaignId);
    assertThat(r.amountRaised()).isEqualByComparingTo("18750");
    assertThat(r.remainingToGoal()).isEqualByComparingTo("6250");
    assertThat(r.percentageOfGoal()).isEqualByComparingTo("75.00");
  }

  @Test
  void exceededGoalHasZeroRemainingAndProgressAboveOneHundred() {
    stubCampaign("100", "125", 2);
    var r = service.campaign(org, campaignId);
    assertThat(r.remainingToGoal()).isEqualByComparingTo("0");
    assertThat(r.percentageOfGoal()).isEqualByComparingTo("125.00");
  }

  @Test
  void zeroGoalHasZeroPercentage() {
    stubCampaign("0", "10", 1);
    assertThat(service.campaign(org, campaignId).percentageOfGoal()).isEqualByComparingTo("0.00");
  }

  @Test
  void summaryCalculatesRestrictedAndUnrestricted() {
    when(donations.summary(org, DonationStatus.RECORDED, min, max))
        .thenReturn(
            List.<Object[]>of(
                new Object[] {
                  3L,
                  new BigDecimal("18750"),
                  new BigDecimal("6250"),
                  new BigDecimal("3750"),
                  2L,
                  LocalDate.now()
                }));
    when(campaigns.countByOrganizationIdAndStatus(org, CampaignStatus.ACTIVE)).thenReturn(1L);
    var r = service.summary(org, null, null);
    assertThat(r.totalAmount()).isEqualByComparingTo("18750");
    assertThat(r.restrictedAmount()).isEqualByComparingTo("3750");
    assertThat(r.unrestrictedAmount()).isEqualByComparingTo("15000");
    assertThat(r.uniqueDonors()).isEqualTo(2);
  }

  @Test
  void zeroDataSummaryUsesExactZeros() {
    when(donations.summary(org, DonationStatus.RECORDED, min, max))
        .thenReturn(
            List.<Object[]>of(
                new Object[] {0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L, null}));
    var r = service.summary(org, null, null);
    assertThat(r.totalDonations()).isZero();
    assertThat(r.averageDonation()).isEqualByComparingTo("0.00");
  }

  @Test
  void paymentMethodAggregationIsExact() {
    when(donations.byPaymentMethod(org, DonationStatus.RECORDED, min, max))
        .thenReturn(
            List.of(
                new Object[] {DonationPaymentMethod.ACH, new BigDecimal("10000"), 1L},
                new Object[] {DonationPaymentMethod.CARD, new BigDecimal("5000"), 1L}));
    assertThat(service.byPayment(org, null, null))
        .extracting("amount")
        .containsExactly(new BigDecimal("10000.00"), new BigDecimal("5000.00"));
  }

  @Test
  void donorHistoryIncludesTotalsAndPagedRecords() {
    UUID donorId = UUID.randomUUID();
    when(donors.findByIdAndOrganizationId(donorId, org)).thenReturn(Optional.of(mock(Donor.class)));
    when(donations.donorTotals(org, donorId, DonationStatus.RECORDED))
        .thenReturn(
            List.<Object[]>of(
                new Object[] {new BigDecimal("15000"), 2L, LocalDate.of(2030, 10, 1)}));
    when(donations.findAll(ArgumentMatchers.<Specification<Donation>>any(), any(Pageable.class)))
        .thenReturn(Page.empty());
    var result = service.donorHistory(org, donorId, PageRequest.of(0, 20));
    assertThat(result.totalContributed()).isEqualByComparingTo("15000.00");
    assertThat(result.donationCount()).isEqualTo(2);
    assertThat(result.mostRecentDonationDate()).isEqualTo(LocalDate.of(2030, 10, 1));
  }

  private void stubCampaign(String goal, String raised, long count) {
    DonationCampaign c = mock(DonationCampaign.class);
    when(c.getId()).thenReturn(campaignId);
    when(c.getName()).thenReturn("Food Drive");
    when(c.getGoalAmount()).thenReturn(new BigDecimal(goal));
    when(campaigns.findByIdAndOrganizationId(campaignId, org)).thenReturn(Optional.of(c));
    when(donations.campaignTotals(org, campaignId, DonationStatus.RECORDED))
        .thenReturn(List.<Object[]>of(new Object[] {new BigDecimal(raised), count}));
  }
}
