package org.civicops.board.reporting;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.civicops.board.security.BoardAccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/board-reports")
@Tag(name = "Board Reporting")
public class BoardReportingController {
  private final BoardReportingService reports;
  private final BoardAccessService access;

  public BoardReportingController(BoardReportingService r, BoardAccessService a) {
    reports = r;
    access = a;
  }

  @GetMapping("/summary")
  public BoardReportDtos.Summary summary(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requireGovernanceReporting(organizationId);
    return reports.summary(organizationId, from, to);
  }

  @GetMapping("/attendance")
  public BoardReportDtos.Attendance attendance(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requireBoardManagement(organizationId);
    return reports.attendance(organizationId, from, to);
  }

  @GetMapping("/voting")
  public BoardReportDtos.Voting voting(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requireGovernanceReporting(organizationId);
    return reports.voting(organizationId, from, to);
  }
}
