package org.civicops.volunteers.reporting;

import java.time.LocalDate;
import java.util.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.civicops.volunteers.reporting.dto.VolunteerHoursTotal;
import org.civicops.volunteers.reporting.dto.VolunteerSummaryReport;
import org.civicops.volunteers.security.VolunteerAccessService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/volunteer-reports")
public class VolunteerReportingController {
  private final VolunteerReportingService service;
  private final VolunteerAccessService access;

  public VolunteerReportingController(VolunteerReportingService s, VolunteerAccessService a) {
    service = s;
    access = a;
  }

  @GetMapping("/summary")
  public VolunteerSummaryReport summary(
      @PathVariable UUID organizationId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    access.requireRead(organizationId);
    if (to.isBefore(from))
      throw new BusinessRuleException(
          "INVALID_DATE_RANGE", "Report end date must not precede start date");
    return service.summary(organizationId, from, to);
  }

  @GetMapping("/volunteers/{volunteerId}/hours")
  public VolunteerHoursTotal volunteerHours(
      @PathVariable UUID organizationId,
      @PathVariable UUID volunteerId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    access.requireRead(organizationId);
    validate(from, to);
    return service.forVolunteer(organizationId, volunteerId, from, to);
  }

  @GetMapping("/hours-by-volunteer")
  public List<VolunteerHoursTotal> grouped(
      @PathVariable UUID organizationId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    access.requireRead(organizationId);
    validate(from, to);
    return service.grouped(organizationId, from, to);
  }

  private void validate(LocalDate from, LocalDate to) {
    if (to.isBefore(from))
      throw new BusinessRuleException(
          "INVALID_DATE_RANGE", "Report end date must not precede start date");
  }
}
