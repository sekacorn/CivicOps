package org.civicops.scholarships.application;

import java.time.*;
import java.util.*;
import org.civicops.scholarships.applicant.*;
import org.civicops.scholarships.application.dto.*;
import org.civicops.scholarships.program.*;
import org.civicops.shared.exception.*;
import org.civicops.shared.finance.Money;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScholarshipApplicationService {
  private final ScholarshipApplicationRepository applications;
  private final ScholarshipProgramService programs;
  private final ScholarshipApplicantService applicants;
  private final Clock clock;

  public ScholarshipApplicationService(
      ScholarshipApplicationRepository a,
      ScholarshipProgramService p,
      ScholarshipApplicantService applicants,
      Clock c) {
    applications = a;
    programs = p;
    this.applicants = applicants;
    clock = c;
  }

  @Transactional
  public ScholarshipApplicationDetailResponse create(
      UUID org, UUID programId, CreateScholarshipApplicationRequest r) {
    ScholarshipProgram p = programs.require(org, programId);
    if (p.getStatus() != ScholarshipProgramStatus.DRAFT
        && p.getStatus() != ScholarshipProgramStatus.OPEN)
      throw new BusinessRuleException(
          "PROGRAM_NOT_ACCEPTING_DRAFTS", "Program is not accepting applications");
    ScholarshipApplicant applicant = applicants.require(org, r.applicantId());
    if (applications.existsByProgramIdAndApplicantId(programId, r.applicantId()))
      throw new ConflictException(
          "DUPLICATE_SCHOLARSHIP_APPLICATION",
          "Applicant already has an application for this program");
    return ScholarshipApplicationDetailResponse.from(
        applications.save(
            new ScholarshipApplication(
                p,
                applicant,
                Boolean.TRUE.equals(r.eligibilityConfirmed()),
                clean(r.eligibilityNotes()),
                clean(r.personalStatement()),
                clean(r.financialNeedStatement()),
                r.gpa(),
                money(r.householdIncome()),
                money(r.requestedAmount()))));
  }

  @Transactional
  public ScholarshipApplicationDetailResponse update(
      UUID org, UUID id, UpdateScholarshipApplicationRequest r) {
    ScholarshipApplication a = require(org, id);
    a.update(
        r.eligibilityConfirmed(),
        clean(r.eligibilityNotes()),
        clean(r.personalStatement()),
        clean(r.financialNeedStatement()),
        r.gpa(),
        money(r.householdIncome()),
        money(r.requestedAmount()));
    return ScholarshipApplicationDetailResponse.from(a);
  }

  @Transactional
  public ScholarshipApplicationDetailResponse submit(UUID org, UUID id) {
    ScholarshipApplication a = require(org, id);
    ScholarshipProgram p = a.getProgram();
    LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
    if (p.getStatus() != ScholarshipProgramStatus.OPEN)
      throw new BusinessRuleException(
          "PROGRAM_NOT_OPEN", "Scholarship program must be OPEN for submission");
    if (today.isBefore(p.getApplicationOpenDate()) || today.isAfter(p.getApplicationDeadline()))
      throw new BusinessRuleException(
          "APPLICATION_WINDOW_CLOSED", "Application submission is outside the program window");
    if (a.getPersonalStatement() == null || a.getPersonalStatement().isBlank())
      throw new BusinessRuleException(
          "APPLICATION_INCOMPLETE", "Personal statement is required for submission");
    a.submit(Instant.now(clock));
    return ScholarshipApplicationDetailResponse.from(a);
  }

  @Transactional
  public ScholarshipApplicationDetailResponse withdraw(UUID org, UUID id) {
    ScholarshipApplication a = require(org, id);
    a.withdraw(Instant.now(clock));
    return ScholarshipApplicationDetailResponse.from(a);
  }

  @Transactional
  public ScholarshipApplicationDetailResponse finalist(UUID org, UUID id) {
    ScholarshipApplication a = require(org, id);
    if (a.getProgram().getStatus() != ScholarshipProgramStatus.REVIEWING)
      throw new BusinessRuleException("PROGRAM_NOT_REVIEWING", "Program must be REVIEWING");
    if (!a.isEligibilityConfirmed())
      throw new BusinessRuleException(
          "APPLICATION_NOT_ELIGIBLE", "Only eligible applications can become finalists");
    a.finalist();
    return ScholarshipApplicationDetailResponse.from(a);
  }

  @Transactional
  public ScholarshipApplicationDetailResponse select(UUID org, UUID id) {
    ScholarshipApplication a = require(org, id);
    ScholarshipProgram locked = programs.requireLocked(org, a.getProgram().getId());
    if (locked.getStatus() != ScholarshipProgramStatus.REVIEWING)
      throw new BusinessRuleException("PROGRAM_NOT_REVIEWING", "Program must be REVIEWING");
    if (!a.isEligibilityConfirmed())
      throw new BusinessRuleException(
          "APPLICATION_NOT_ELIGIBLE", "Only eligible finalists can be selected");
    Integer limit = locked.getNumberOfAwards();
    long selected =
        applications.countByProgramIdAndStatus(
            locked.getId(), ScholarshipApplicationStatus.SELECTED);
    if (limit != null && selected >= limit)
      throw new BusinessRuleException(
          "SCHOLARSHIP_AWARD_LIMIT_REACHED", "Program selection limit has been reached");
    a.select();
    return ScholarshipApplicationDetailResponse.from(a);
  }

  @Transactional
  public ScholarshipApplicationDetailResponse notSelect(UUID org, UUID id) {
    ScholarshipApplication a = require(org, id);
    if (a.getProgram().getStatus() != ScholarshipProgramStatus.REVIEWING)
      throw new BusinessRuleException("PROGRAM_NOT_REVIEWING", "Program must be REVIEWING");
    a.notSelect();
    return ScholarshipApplicationDetailResponse.from(a);
  }

  @Transactional(readOnly = true)
  public ScholarshipApplication require(UUID org, UUID id) {
    return applications
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Scholarship application", id));
  }

  @Transactional(readOnly = true)
  public ScholarshipApplicationDetailResponse detail(UUID org, UUID id) {
    return ScholarshipApplicationDetailResponse.from(require(org, id));
  }

  @Transactional(readOnly = true)
  public Page<ScholarshipApplicationSummaryResponse> list(
      UUID org,
      UUID program,
      ScholarshipApplicationStatus status,
      UUID applicant,
      Instant from,
      Instant to,
      Boolean eligible,
      Pageable pageable) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Submission filter end precedes start");
    Specification<ScholarshipApplication> s =
        (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (program != null) s = s.and((r, q, c) -> c.equal(r.get("program").get("id"), program));
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (applicant != null) s = s.and((r, q, c) -> c.equal(r.get("applicant").get("id"), applicant));
    if (from != null) s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("submittedAt"), from));
    if (to != null) s = s.and((r, q, c) -> c.lessThanOrEqualTo(r.get("submittedAt"), to));
    if (eligible != null) s = s.and((r, q, c) -> c.equal(r.get("eligibilityConfirmed"), eligible));
    return applications.findAll(s, pageable).map(ScholarshipApplicationSummaryResponse::from);
  }

  private static java.math.BigDecimal money(java.math.BigDecimal x) {
    return x == null ? null : Money.amount(x);
  }

  private static String clean(String x) {
    return x == null || x.isBlank() ? null : x.trim();
  }
}
