package org.civicops.events.reporting;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.*;
import org.civicops.events.reporting.dto.*;
import org.civicops.events.security.EventAccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
@Tag(name = "Event Reporting")
public class EventReportingController {
  private final EventReportingService service;
  private final EventAccessService access;

  public EventReportingController(EventReportingService s, EventAccessService a) {
    service = s;
    access = a;
  }

  @GetMapping("/event-reports/summary")
  public EventSummaryReportResponse summary(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requireRead(organizationId);
    return service.summary(organizationId, from, to);
  }

  @GetMapping("/events/{eventId}/report")
  public EventReportResponse report(@PathVariable UUID organizationId, @PathVariable UUID eventId) {
    access.requireRead(organizationId);
    return service.report(organizationId, eventId);
  }
}
