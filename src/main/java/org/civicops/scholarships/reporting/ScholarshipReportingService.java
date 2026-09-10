package org.civicops.scholarships.reporting;

import java.math.*;
import java.util.*;
import org.civicops.scholarships.application.*;
import org.civicops.scholarships.award.*;
import org.civicops.scholarships.program.*;
import org.civicops.scholarships.reporting.dto.*;
import org.civicops.scholarships.review.*;
import org.civicops.shared.finance.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScholarshipReportingService {
  private final ScholarshipProgramRepository programs;
  private final ScholarshipApplicationRepository applications;
  private final ScholarshipAwardRepository awards;
  private final ScholarshipReviewAssignmentRepository assignments;
  private final ScholarshipReviewRepository reviews;

  public ScholarshipReportingService(
      ScholarshipProgramRepository p,
      ScholarshipApplicationRepository a,
      ScholarshipAwardRepository awards,
      ScholarshipReviewAssignmentRepository assignments,
      ScholarshipReviewRepository reviews) {
    programs = p;
    applications = a;
    this.awards = awards;
    this.assignments = assignments;
    this.reviews = reviews;
  }

  @Transactional(readOnly = true)
  public ScholarshipSummaryResponse summary(UUID org) {
    long submitted = count(org, ScholarshipApplicationStatus.SUBMITTED),
        under = count(org, ScholarshipApplicationStatus.UNDER_REVIEW),
        finalists = count(org, ScholarshipApplicationStatus.FINALIST),
        selected = count(org, ScholarshipApplicationStatus.SELECTED);
    return new ScholarshipSummaryResponse(
        org,
        programs.countByOrganizationIdAndStatusNotIn(
            org, List.of(ScholarshipProgramStatus.AWARDED, ScholarshipProgramStatus.CANCELLED)),
        submitted,
        under,
        finalists,
        selected,
        awards.countByOrganizationId(org),
        Money.amount(awards.totalByOrganization(org)));
  }

  @Transactional(readOnly = true)
  public ScholarshipProgramReportResponse program(UUID org, UUID id) {
    ScholarshipProgram p =
        programs
            .findByIdAndOrganizationId(id, org)
            .orElseThrow(
                () ->
                    new org.civicops.shared.exception.ResourceNotFoundException(
                        "Scholarship program", id));
    List<ScholarshipApplication> rows =
        applications.findAll(
            (root, q, c) ->
                c.and(
                    c.equal(root.get("organization").get("id"), org),
                    c.equal(root.get("program").get("id"), id)));
    return new ScholarshipProgramReportResponse(
        id,
        p.getName(),
        rows.size(),
        status(rows, ScholarshipApplicationStatus.SUBMITTED),
        rows.stream().filter(ScholarshipApplication::isEligibilityConfirmed).count(),
        status(rows, ScholarshipApplicationStatus.UNDER_REVIEW),
        status(rows, ScholarshipApplicationStatus.FINALIST),
        status(rows, ScholarshipApplicationStatus.SELECTED),
        status(rows, ScholarshipApplicationStatus.NOT_SELECTED),
        awards.countByProgramId(id),
        Money.amount(awards.totalByProgram(id)));
  }

  @Transactional(readOnly = true)
  public ScholarshipReviewReportResponse reviews(UUID org) {
    long assigned = assignments.countByOrganizationId(org),
        completed =
            assignments.countByOrganizationIdAndStatus(org, ReviewAssignmentStatus.COMPLETED);
    BigDecimal avg = reviews.averageScore(org);
    return new ScholarshipReviewReportResponse(
        org,
        assigned,
        completed,
        assigned - completed,
        avg == null ? BigDecimal.ZERO.setScale(2) : avg.setScale(2, RoundingMode.HALF_UP));
  }

  private long count(UUID org, ScholarshipApplicationStatus s) {
    return applications.countByOrganizationIdAndStatus(org, s);
  }

  private static long status(List<ScholarshipApplication> rows, ScholarshipApplicationStatus s) {
    return rows.stream().filter(a -> a.getStatus() == s).count();
  }
}
