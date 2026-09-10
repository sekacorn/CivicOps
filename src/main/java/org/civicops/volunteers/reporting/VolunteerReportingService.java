package org.civicops.volunteers.reporting;

import java.time.*;
import java.util.*;
import org.civicops.volunteers.hours.VolunteerHourEntryRepository;
import org.civicops.volunteers.opportunity.*;
import org.civicops.volunteers.reporting.dto.VolunteerHoursTotal;
import org.civicops.volunteers.reporting.dto.VolunteerSummaryReport;
import org.civicops.volunteers.shift.VolunteerShiftRepository;
import org.civicops.volunteers.volunteer.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VolunteerReportingService {
  private final VolunteerRepository volunteers;
  private final VolunteerHourEntryRepository hours;
  private final VolunteerShiftRepository shifts;
  private final VolunteerOpportunityRepository opportunities;
  private final Clock clock;

  public VolunteerReportingService(
      VolunteerRepository v,
      VolunteerHourEntryRepository h,
      VolunteerShiftRepository s,
      VolunteerOpportunityRepository o,
      Clock c) {
    volunteers = v;
    hours = h;
    shifts = s;
    opportunities = o;
    clock = c;
  }

  @Transactional(readOnly = true)
  public VolunteerSummaryReport summary(UUID orgId, LocalDate from, LocalDate to) {
    if (to.isBefore(from)) throw new IllegalArgumentException("to must not precede from");
    return new VolunteerSummaryReport(
        from,
        to,
        volunteers.countByOrganizationIdAndStatus(orgId, VolunteerStatus.ACTIVE),
        hours.approvedTotal(orgId, from, to),
        shifts.countByOrganizationIdAndStartAtAfter(orgId, clock.instant()),
        opportunities.countByOrganizationIdAndStatus(orgId, OpportunityStatus.OPEN),
        shifts.openCapacity(orgId, clock.instant()));
  }

  @Transactional(readOnly = true)
  public VolunteerHoursTotal forVolunteer(
      UUID orgId, UUID volunteerId, LocalDate from, LocalDate to) {
    volunteers
        .findByIdAndOrganizationId(volunteerId, orgId)
        .orElseThrow(
            () ->
                new org.civicops.shared.exception.ResourceNotFoundException(
                    "Volunteer", volunteerId));
    return new VolunteerHoursTotal(
        volunteerId, hours.approvedTotalForVolunteer(orgId, volunteerId, from, to));
  }

  @Transactional(readOnly = true)
  public List<VolunteerHoursTotal> grouped(UUID orgId, LocalDate from, LocalDate to) {
    return hours.approvedTotalsByVolunteer(orgId, from, to).stream()
        .map(row -> new VolunteerHoursTotal((UUID) row[0], (java.math.BigDecimal) row[1]))
        .toList();
  }
}
