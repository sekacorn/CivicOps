package org.civicops.events.reporting;

import java.math.*;
import java.time.*;
import java.util.*;
import org.civicops.donations.reporting.*;
import org.civicops.donations.reporting.dto.CampaignFinancialSummaryResponse;
import org.civicops.events.event.*;
import org.civicops.events.registration.*;
import org.civicops.events.reporting.dto.*;
import org.civicops.shared.finance.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventReportingService {
  private final EventRecordRepository events;
  private final EventRegistrationRepository registrations;
  private final DonationReportingService donations;
  private final Clock clock;

  public EventReportingService(
      EventRecordRepository e, EventRegistrationRepository r, DonationReportingService d, Clock c) {
    events = e;
    registrations = r;
    donations = d;
    clock = c;
  }

  @Transactional(readOnly = true)
  public EventSummaryReportResponse summary(UUID org) {
    return summary(org, null, null);
  }

  @Transactional(readOnly = true)
  public EventSummaryReportResponse summary(UUID org, LocalDate from, LocalDate to) {
    if (from != null && to != null && to.isBefore(from))
      throw new org.civicops.shared.exception.BusinessRuleException(
          "INVALID_DATE_RANGE", "Report end must not precede start");
    Instant lower =
        (from == null ? LocalDate.of(1, 1, 1) : from).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant upper =
        (to == null ? LocalDate.of(9999, 12, 31) : to.plusDays(1))
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant();
    List<EventRecord> selected =
        events.findByOrganizationIdAndStartDateTimeBetween(org, lower, upper);
    long open =
        selected.stream().filter(e -> e.getStatus() == EventStatus.REGISTRATION_OPEN).count();
    long registered =
        selected.stream()
            .mapToLong(
                e ->
                    registrations.countByEventIdAndStatus(e.getId(), RegistrationStatus.REGISTERED)
                        + registrations.countByEventIdAndStatus(
                            e.getId(), RegistrationStatus.ATTENDED))
            .sum();
    long waitlisted =
        selected.stream()
            .mapToLong(
                e ->
                    registrations.countByEventIdAndStatus(e.getId(), RegistrationStatus.WAITLISTED))
            .sum();
    long completed = selected.stream().filter(e -> e.getStatus() == EventStatus.COMPLETED).count();
    return new EventSummaryReportResponse(selected.size(), open, registered, waitlisted, completed);
  }

  @Transactional(readOnly = true)
  public EventReportResponse report(UUID org, UUID eventId) {
    EventRecord e =
        events
            .findByIdAndOrganizationId(eventId, org)
            .orElseThrow(
                () ->
                    new org.civicops.shared.exception.ResourceNotFoundException("Event", eventId));
    long registered = registrations.countByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED),
        waitlisted = registrations.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED),
        attended = registrations.countByEventIdAndStatus(eventId, RegistrationStatus.ATTENDED),
        noShow = registrations.countByEventIdAndStatus(eventId, RegistrationStatus.NO_SHOW),
        cancelled = registrations.countByEventIdAndStatus(eventId, RegistrationStatus.CANCELLED);
    long seated = registered + attended;
    Integer remaining =
        e.getCapacity() == null ? null : (int) Math.max(0, e.getCapacity() - seated);
    BigDecimal rate =
        seated == 0
            ? Money.amount(BigDecimal.ZERO)
            : Money.percent(BigDecimal.valueOf(attended), BigDecimal.valueOf(seated));
    CampaignFinancialSummaryResponse c =
        e.getLinkedDonationCampaign() == null
            ? null
            : donations.campaign(org, e.getLinkedDonationCampaign().getId());
    return new EventReportResponse(
        eventId,
        e.getCapacity(),
        registered,
        waitlisted,
        attended,
        noShow,
        cancelled,
        remaining,
        rate,
        e.getLinkedGrant() == null ? null : e.getLinkedGrant().getId(),
        e.getLinkedGrant() == null ? null : e.getLinkedGrant().getGrantName(),
        c == null ? null : c.campaignId(),
        c == null ? null : c.campaignName(),
        c == null ? null : c.goalAmount(),
        c == null ? null : c.amountRaised(),
        c == null ? 0 : c.donationCount());
  }
}
