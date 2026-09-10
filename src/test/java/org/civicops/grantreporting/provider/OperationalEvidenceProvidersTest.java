package org.civicops.grantreporting.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.civicops.cases.reporting.CaseReportingService;
import org.civicops.donations.campaign.DonationCampaign;
import org.civicops.donations.reporting.DonationReportingService;
import org.civicops.events.event.*;
import org.civicops.events.reporting.EventReportingService;
import org.civicops.foodpantry.reporting.FoodPantryReportingService;
import org.civicops.grantreporting.evidence.*;
import org.civicops.grantreporting.provider.GrantReportEvidenceProvider.EvidenceContext;
import org.civicops.grants.grant.Grant;
import org.civicops.grants.reporting.GrantReportingService;
import org.civicops.scholarships.reporting.ScholarshipReportingService;
import org.civicops.volunteers.hours.VolunteerHourEntryRepository;
import org.civicops.volunteers.opportunity.VolunteerOpportunityRepository;
import org.junit.jupiter.api.Test;

class OperationalEvidenceProvidersTest {
  private final UUID organizationId = UUID.randomUUID();
  private final UUID grantId = UUID.randomUUID();
  private final LocalDate from = LocalDate.of(2026, 1, 1);
  private final LocalDate to = LocalDate.of(2026, 6, 30);
  private final Grant grant = mock(Grant.class);
  private final EvidenceContext context = new EvidenceContext(organizationId, grant, from, to);

  OperationalEvidenceProvidersTest() {
    when(grant.getId()).thenReturn(grantId);
  }

  @Test
  void grantProviderUsesAuthoritativeFinancialReportWithoutRecalculation() {
    GrantReportingService reports = mock(GrantReportingService.class);
    var financial = mock(org.civicops.grants.reporting.dto.GrantFinancialSummaryResponse.class);
    when(financial.awardAmount()).thenReturn(new BigDecimal("50000.00"));
    when(financial.totalSpent()).thenReturn(new BigDecimal("12500.00"));
    when(financial.remainingBalance()).thenReturn(new BigDecimal("37500.00"));
    when(financial.utilizationPercent()).thenReturn(new BigDecimal("25.00"));
    when(reports.financial(organizationId, grantId)).thenReturn(financial);
    var evidence = new GrantEvidenceProvider(reports).collect(context);
    assertThat(evidence)
        .extracting(EvidenceDraft::metric)
        .containsExactly(
            GrantReportMetric.GRANT_AWARD_AMOUNT, GrantReportMetric.GRANT_TOTAL_SPENT,
            GrantReportMetric.GRANT_REMAINING_BALANCE, GrantReportMetric.GRANT_UTILIZATION_PERCENT);
    assertThat(evidence.get(1).monetaryValue()).isEqualByComparingTo("12500.00");
    verify(reports).financial(organizationId, grantId);
  }

  @Test
  void eventProviderIncludesOnlyGrantLinkedEventsInInclusivePeriod() {
    EventRecordRepository events = mock(EventRecordRepository.class);
    EventReportingService reports = mock(EventReportingService.class);
    EventRecord linked = mock(EventRecord.class), unrelated = mock(EventRecord.class);
    Grant otherGrant = mock(Grant.class);
    when(otherGrant.getId()).thenReturn(UUID.randomUUID());
    UUID linkedId = UUID.randomUUID();
    when(linked.getLinkedGrant()).thenReturn(grant);
    when(linked.getStatus()).thenReturn(EventStatus.COMPLETED);
    when(linked.getId()).thenReturn(linkedId);
    when(unrelated.getLinkedGrant()).thenReturn(otherGrant);
    when(events.findByOrganizationIdAndStartDateTimeBetween(eq(organizationId), any(), any()))
        .thenReturn(List.of(linked, unrelated));
    var eventReport = mock(org.civicops.events.reporting.dto.EventReportResponse.class);
    when(eventReport.attended()).thenReturn(8L);
    when(eventReport.registered()).thenReturn(2L);
    when(reports.report(organizationId, linkedId)).thenReturn(eventReport);
    var evidence = new EventEvidenceProvider(events, reports).collect(context);
    assertThat(evidence)
        .extracting(EvidenceDraft::numericValue)
        .containsExactly(new BigDecimal("1"), new BigDecimal("8"), new BigDecimal("80.00"));
    verify(reports).report(organizationId, linkedId);
    verifyNoMoreInteractions(reports);
  }

  @Test
  void donationProviderUsesOnlyCampaignsLinkedThroughGrantEvents() {
    EventRecordRepository events = mock(EventRecordRepository.class);
    DonationReportingService reports = mock(DonationReportingService.class);
    EventRecord event = mock(EventRecord.class);
    DonationCampaign campaign = mock(DonationCampaign.class);
    UUID campaignId = UUID.randomUUID();
    when(campaign.getId()).thenReturn(campaignId);
    when(event.getLinkedGrant()).thenReturn(grant);
    when(event.getLinkedDonationCampaign()).thenReturn(campaign);
    when(events.findByOrganizationIdAndStartDateTimeBetween(eq(organizationId), any(), any()))
        .thenReturn(List.of(event));
    var campaignReport =
        mock(org.civicops.donations.reporting.dto.CampaignFinancialSummaryResponse.class);
    when(campaignReport.amountRaised()).thenReturn(new BigDecimal("2400.00"));
    when(campaignReport.donationCount()).thenReturn(12L);
    when(reports.campaign(organizationId, campaignId)).thenReturn(campaignReport);
    var evidence = new DonationEvidenceProvider(events, reports).collect(context);
    assertThat(evidence.get(0).monetaryValue()).isEqualByComparingTo("2400.00");
    assertThat(evidence.get(1).numericValue()).isEqualByComparingTo("12");
    verify(reports).campaign(organizationId, campaignId);
  }

  @Test
  void volunteerProviderUsesOnlyApprovedGrantLinkedHoursForPeriod() {
    VolunteerOpportunityRepository opportunities = mock(VolunteerOpportunityRepository.class);
    VolunteerHourEntryRepository hours = mock(VolunteerHourEntryRepository.class);
    when(opportunities.countByOrganizationIdAndEventLinkedGrantId(organizationId, grantId))
        .thenReturn(1L);
    when(hours.approvedTotalForGrantLinkedActivities(organizationId, grantId, from, to))
        .thenReturn(new BigDecimal("120.500"));
    var evidence = new VolunteerEvidenceProvider(opportunities, hours).collect(context);
    assertThat(evidence)
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.metric()).isEqualTo(GrantReportMetric.VOLUNTEER_APPROVED_HOURS);
              assertThat(e.numericValue()).isEqualByComparingTo("120.500");
            });
  }

  @Test
  void caseProviderUsesOnlyAggregatePeriodReporting() {
    CaseReportingService reports = mock(CaseReportingService.class);
    var summary = mock(org.civicops.cases.reporting.dto.CaseReportSummaryResponse.class);
    var services = mock(org.civicops.cases.reporting.dto.CaseServiceTypeReportResponse.class);
    when(summary.casesClosedInPeriod()).thenReturn(7L);
    when(services.serviceCount()).thenReturn(19L);
    when(reports.summary(organizationId, from, to)).thenReturn(summary);
    when(reports.services(organizationId, from, to)).thenReturn(List.of(services));
    var evidence = new CaseEvidenceProvider(reports).collect(context);
    assertThat(evidence)
        .extracting(EvidenceDraft::numericValue)
        .containsExactly(new BigDecimal("7"), new BigDecimal("19"));
    assertThat(evidence).allSatisfy(e -> assertThat(e.sourceId()).isNull());
  }

  @Test
  void scholarshipProviderUsesPiiFreePointInTimeSummary() {
    ScholarshipReportingService reports = mock(ScholarshipReportingService.class);
    var summary = mock(org.civicops.scholarships.reporting.dto.ScholarshipSummaryResponse.class);
    when(summary.applicationsSubmitted()).thenReturn(30L);
    when(summary.awardsOffered()).thenReturn(4L);
    when(summary.totalAwarded()).thenReturn(new BigDecimal("10000.00"));
    when(reports.summary(organizationId)).thenReturn(summary);
    var evidence = new ScholarshipEvidenceProvider(reports).collect(context);
    assertThat(evidence)
        .extracting(EvidenceDraft::metric)
        .containsExactly(
            GrantReportMetric.SCHOLARSHIP_APPLICATIONS_SUBMITTED,
            GrantReportMetric.SCHOLARSHIP_AWARDS_COUNT,
            GrantReportMetric.SCHOLARSHIP_TOTAL_AWARDED);
    assertThat(evidence.get(2).monetaryValue()).isEqualByComparingTo("10000.00");
  }

  @Test
  void pantryProviderUsesAggregateInclusivePeriodReport() {
    FoodPantryReportingService reports = mock(FoodPantryReportingService.class);
    var distribution =
        mock(org.civicops.foodpantry.reporting.dto.PantryDistributionReportResponse.class);
    when(distribution.uniqueHouseholdsServed()).thenReturn(14L);
    when(distribution.visitsCompleted()).thenReturn(22L);
    when(distribution.totalQuantityDistributed()).thenReturn(new BigDecimal("480.000"));
    when(reports.distributions(organizationId, null, from, to)).thenReturn(distribution);
    var evidence = new FoodPantryEvidenceProvider(reports).collect(context);
    assertThat(evidence)
        .extracting(EvidenceDraft::numericValue)
        .containsExactly(new BigDecimal("14"), new BigDecimal("22"), new BigDecimal("480.000"));
    assertThat(evidence).allSatisfy(e -> assertThat(e.sourceId()).isNull());
  }
}
