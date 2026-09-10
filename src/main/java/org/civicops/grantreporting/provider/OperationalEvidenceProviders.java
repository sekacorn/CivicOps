package org.civicops.grantreporting.provider;

import java.math.*;
import java.time.*;
import java.util.*;
import org.civicops.cases.reporting.CaseReportingService;
import org.civicops.donations.reporting.DonationReportingService;
import org.civicops.events.event.*;
import org.civicops.events.reporting.EventReportingService;
import org.civicops.foodpantry.reporting.FoodPantryReportingService;
import org.civicops.grantreporting.evidence.*;
import org.civicops.grants.reporting.GrantReportingService;
import org.civicops.scholarships.reporting.ScholarshipReportingService;
import org.civicops.shared.finance.Money;
import org.civicops.volunteers.hours.VolunteerHourEntryRepository;
import org.civicops.volunteers.opportunity.VolunteerOpportunityRepository;
import org.springframework.stereotype.Component;

@Component
class GrantEvidenceProvider implements GrantReportEvidenceProvider {
  private final GrantReportingService reports;

  GrantEvidenceProvider(GrantReportingService r) {
    reports = r;
  }

  public EvidenceSourceModule sourceModule() {
    return EvidenceSourceModule.GRANT;
  }

  public List<EvidenceDraft> collect(EvidenceContext c) {
    var x = reports.financial(c.organizationId(), c.grant().getId());
    String ref = "grant:" + c.grant().getId();
    return List.of(
        EvidenceDraft.money(
            sourceModule(),
            "GRANT",
            c.grant().getId(),
            GrantReportMetric.GRANT_AWARD_AMOUNT,
            x.awardAmount(),
            ref),
        EvidenceDraft.money(
            sourceModule(),
            "GRANT",
            c.grant().getId(),
            GrantReportMetric.GRANT_TOTAL_SPENT,
            x.totalSpent(),
            ref),
        EvidenceDraft.money(
            sourceModule(),
            "GRANT",
            c.grant().getId(),
            GrantReportMetric.GRANT_REMAINING_BALANCE,
            x.remainingBalance(),
            ref),
        EvidenceDraft.number(
            sourceModule(),
            "GRANT",
            c.grant().getId(),
            GrantReportMetric.GRANT_UTILIZATION_PERCENT,
            x.utilizationPercent(),
            "percent",
            ref));
  }
}

@Component
class EventEvidenceProvider implements GrantReportEvidenceProvider {
  private final EventRecordRepository events;
  private final EventReportingService reports;

  EventEvidenceProvider(EventRecordRepository e, EventReportingService r) {
    events = e;
    reports = r;
  }

  public EvidenceSourceModule sourceModule() {
    return EvidenceSourceModule.EVENTS;
  }

  public List<EvidenceDraft> collect(EvidenceContext c) {
    Instant from = c.from().atStartOfDay(ZoneOffset.UTC).toInstant(),
        to = c.to().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    List<EventRecord> linked =
        events.findByOrganizationIdAndStartDateTimeBetween(c.organizationId(), from, to).stream()
            .filter(
                e ->
                    e.getLinkedGrant() != null
                        && e.getLinkedGrant().getId().equals(c.grant().getId()))
            .toList();
    if (linked.isEmpty())
      return List.of(
          EvidenceDraft.missing(
              sourceModule(),
              GrantReportMetric.EVENT_EVENTS_COMPLETED,
              "No grant-linked events in reporting period"),
          EvidenceDraft.missing(
              sourceModule(),
              GrantReportMetric.EVENT_ATTENDEES,
              "No grant-linked events in reporting period"),
          EvidenceDraft.missing(
              sourceModule(),
              GrantReportMetric.EVENT_ATTENDANCE_RATE,
              "No grant-linked events in reporting period"));
    long completed = linked.stream().filter(e -> e.getStatus() == EventStatus.COMPLETED).count(),
        attended = 0,
        seated = 0;
    for (var e : linked) {
      var r = reports.report(c.organizationId(), e.getId());
      attended += r.attended();
      seated += r.attended() + r.registered();
    }
    BigDecimal rate = Money.percent(BigDecimal.valueOf(attended), BigDecimal.valueOf(seated));
    String ref = "grant-linked-events:" + c.grant().getId();
    return List.of(
        EvidenceDraft.number(
            sourceModule(),
            "EVENT_AGGREGATE",
            c.grant().getId(),
            GrantReportMetric.EVENT_EVENTS_COMPLETED,
            BigDecimal.valueOf(completed),
            "events",
            ref),
        EvidenceDraft.number(
            sourceModule(),
            "EVENT_AGGREGATE",
            c.grant().getId(),
            GrantReportMetric.EVENT_ATTENDEES,
            BigDecimal.valueOf(attended),
            "attendees",
            ref),
        EvidenceDraft.number(
            sourceModule(),
            "EVENT_AGGREGATE",
            c.grant().getId(),
            GrantReportMetric.EVENT_ATTENDANCE_RATE,
            rate,
            "percent",
            ref));
  }
}

@Component
class DonationEvidenceProvider implements GrantReportEvidenceProvider {
  private final EventRecordRepository events;
  private final DonationReportingService reports;

  DonationEvidenceProvider(EventRecordRepository e, DonationReportingService r) {
    events = e;
    reports = r;
  }

  public EvidenceSourceModule sourceModule() {
    return EvidenceSourceModule.DONATIONS;
  }

  public List<EvidenceDraft> collect(EvidenceContext c) {
    List<UUID> campaigns =
        events
            .findByOrganizationIdAndStartDateTimeBetween(
                c.organizationId(), Instant.EPOCH, Instant.parse("9999-12-30T00:00:00Z"))
            .stream()
            .filter(
                e ->
                    e.getLinkedGrant() != null
                        && e.getLinkedGrant().getId().equals(c.grant().getId())
                        && e.getLinkedDonationCampaign() != null)
            .map(e -> e.getLinkedDonationCampaign().getId())
            .distinct()
            .toList();
    if (campaigns.isEmpty())
      return List.of(
          EvidenceDraft.missing(
              sourceModule(),
              GrantReportMetric.DONATION_AMOUNT_RAISED,
              "No campaign linked through a grant event"),
          EvidenceDraft.missing(
              sourceModule(),
              GrantReportMetric.DONATION_COUNT,
              "No campaign linked through a grant event"));
    BigDecimal amount = BigDecimal.ZERO;
    long count = 0;
    for (UUID id : campaigns) {
      var r = reports.campaign(c.organizationId(), id);
      amount = amount.add(r.amountRaised());
      count += r.donationCount();
    }
    String ref =
        "grant-linked-campaigns:"
            + String.join(",", campaigns.stream().map(UUID::toString).toList());
    return List.of(
        EvidenceDraft.money(
            sourceModule(),
            "CAMPAIGN_AGGREGATE",
            c.grant().getId(),
            GrantReportMetric.DONATION_AMOUNT_RAISED,
            Money.amount(amount),
            ref),
        EvidenceDraft.number(
            sourceModule(),
            "CAMPAIGN_AGGREGATE",
            c.grant().getId(),
            GrantReportMetric.DONATION_COUNT,
            BigDecimal.valueOf(count),
            "donations",
            ref));
  }
}

@Component
class VolunteerEvidenceProvider implements GrantReportEvidenceProvider {
  private final VolunteerOpportunityRepository opportunities;
  private final VolunteerHourEntryRepository hours;

  VolunteerEvidenceProvider(VolunteerOpportunityRepository o, VolunteerHourEntryRepository h) {
    opportunities = o;
    hours = h;
  }

  public EvidenceSourceModule sourceModule() {
    return EvidenceSourceModule.VOLUNTEERS;
  }

  public List<EvidenceDraft> collect(EvidenceContext c) {
    if (opportunities.countByOrganizationIdAndEventLinkedGrantId(
            c.organizationId(), c.grant().getId())
        == 0)
      return List.of(
          EvidenceDraft.missing(
              sourceModule(),
              GrantReportMetric.VOLUNTEER_APPROVED_HOURS,
              "No volunteer opportunity linked through a grant event"));
    BigDecimal value =
        hours.approvedTotalForGrantLinkedActivities(
            c.organizationId(), c.grant().getId(), c.from(), c.to());
    return List.of(
        EvidenceDraft.number(
            sourceModule(),
            "LINKED_VOLUNTEER_HOURS",
            c.grant().getId(),
            GrantReportMetric.VOLUNTEER_APPROVED_HOURS,
            value,
            "hours",
            "approved-hours:grant-linked:" + c.grant().getId()));
  }
}

@Component
class CaseEvidenceProvider implements GrantReportEvidenceProvider {
  private final CaseReportingService reports;

  CaseEvidenceProvider(CaseReportingService r) {
    reports = r;
  }

  public EvidenceSourceModule sourceModule() {
    return EvidenceSourceModule.CASES;
  }

  public List<EvidenceDraft> collect(EvidenceContext c) {
    var summary = reports.summary(c.organizationId(), c.from(), c.to());
    long services =
        reports.services(c.organizationId(), c.from(), c.to()).stream()
            .mapToLong(x -> x.serviceCount())
            .sum();
    String ref = "explicit-organization-aggregate:cases";
    return List.of(
        EvidenceDraft.number(
            sourceModule(),
            "EXPLICIT_ORGANIZATION_AGGREGATE",
            null,
            GrantReportMetric.CASE_CASES_CLOSED,
            BigDecimal.valueOf(summary.casesClosedInPeriod()),
            "cases",
            ref),
        EvidenceDraft.number(
            sourceModule(),
            "EXPLICIT_ORGANIZATION_AGGREGATE",
            null,
            GrantReportMetric.CASE_SERVICES_PROVIDED,
            BigDecimal.valueOf(services),
            "services",
            ref));
  }
}

@Component
class ScholarshipEvidenceProvider implements GrantReportEvidenceProvider {
  private final ScholarshipReportingService reports;

  ScholarshipEvidenceProvider(ScholarshipReportingService r) {
    reports = r;
  }

  public EvidenceSourceModule sourceModule() {
    return EvidenceSourceModule.SCHOLARSHIPS;
  }

  public List<EvidenceDraft> collect(EvidenceContext c) {
    var x = reports.summary(c.organizationId());
    String ref = "explicit-organization-point-in-time:scholarships";
    return List.of(
        EvidenceDraft.number(
            sourceModule(),
            "EXPLICIT_ORGANIZATION_AGGREGATE",
            null,
            GrantReportMetric.SCHOLARSHIP_APPLICATIONS_SUBMITTED,
            BigDecimal.valueOf(x.applicationsSubmitted()),
            "applications",
            ref),
        EvidenceDraft.number(
            sourceModule(),
            "EXPLICIT_ORGANIZATION_AGGREGATE",
            null,
            GrantReportMetric.SCHOLARSHIP_AWARDS_COUNT,
            BigDecimal.valueOf(x.awardsOffered()),
            "awards",
            ref),
        EvidenceDraft.money(
            sourceModule(),
            "EXPLICIT_ORGANIZATION_AGGREGATE",
            null,
            GrantReportMetric.SCHOLARSHIP_TOTAL_AWARDED,
            x.totalAwarded(),
            ref));
  }
}

@Component
class FoodPantryEvidenceProvider implements GrantReportEvidenceProvider {
  private final FoodPantryReportingService reports;

  FoodPantryEvidenceProvider(FoodPantryReportingService r) {
    reports = r;
  }

  public EvidenceSourceModule sourceModule() {
    return EvidenceSourceModule.FOOD_PANTRY;
  }

  public List<EvidenceDraft> collect(EvidenceContext c) {
    var d = reports.distributions(c.organizationId(), null, c.from(), c.to());
    String ref = "explicit-organization-aggregate:food-pantry";
    return List.of(
        EvidenceDraft.number(
            sourceModule(),
            "EXPLICIT_ORGANIZATION_AGGREGATE",
            null,
            GrantReportMetric.FOODPANTRY_HOUSEHOLDS_SERVED,
            BigDecimal.valueOf(d.uniqueHouseholdsServed()),
            "households",
            ref),
        EvidenceDraft.number(
            sourceModule(),
            "EXPLICIT_ORGANIZATION_AGGREGATE",
            null,
            GrantReportMetric.FOODPANTRY_VISITS_COMPLETED,
            BigDecimal.valueOf(d.visitsCompleted()),
            "visits",
            ref),
        EvidenceDraft.number(
            sourceModule(),
            "EXPLICIT_ORGANIZATION_AGGREGATE",
            null,
            GrantReportMetric.FOODPANTRY_QUANTITY_DISTRIBUTED,
            d.totalQuantityDistributed(),
            "items",
            ref));
  }
}
