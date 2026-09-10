package org.civicops.scholarships.award;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.civicops.core.user.UserService;
import org.civicops.scholarships.application.*;
import org.civicops.scholarships.award.dto.*;
import org.civicops.shared.exception.*;
import org.civicops.shared.finance.Money;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScholarshipAwardService {
  private final ScholarshipAwardRepository awards;
  private final ScholarshipApplicationService applications;
  private final UserService users;

  public ScholarshipAwardService(
      ScholarshipAwardRepository a, ScholarshipApplicationService apps, UserService u) {
    awards = a;
    applications = apps;
    users = u;
  }

  @Transactional
  public ScholarshipAwardResponse create(
      UUID org, UUID applicationId, UUID actor, CreateScholarshipAwardRequest r) {
    ScholarshipApplication app = applications.require(org, applicationId);
    if (app.getStatus() != ScholarshipApplicationStatus.SELECTED)
      throw new BusinessRuleException(
          "APPLICATION_NOT_SELECTED", "Awards require a selected application");
    if (awards.existsByApplicationId(applicationId))
      throw new ConflictException(
          "DUPLICATE_SCHOLARSHIP_AWARD", "Application already has an award");
    BigDecimal amount = Money.amount(r.amount());
    BigDecimal limit = app.getProgram().getAwardAmount();
    if (limit != null && limit.signum() > 0 && amount.compareTo(limit) > 0)
      throw new BusinessRuleException(
          "AWARD_AMOUNT_EXCEEDS_PROGRAM_LIMIT",
          "Award amount exceeds the program per-award amount");
    return ScholarshipAwardResponse.from(
        awards.save(
            new ScholarshipAward(
                app, amount, r.awardDate(), clean(r.notes()), users.requireEntity(actor))));
  }

  @Transactional
  public ScholarshipAwardResponse transition(UUID org, UUID id, ScholarshipAwardStatus target) {
    ScholarshipAward a = require(org, id);
    a.transition(target);
    return ScholarshipAwardResponse.from(a);
  }

  @Transactional(readOnly = true)
  public ScholarshipAward require(UUID org, UUID id) {
    return awards
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Scholarship award", id));
  }

  @Transactional(readOnly = true)
  public ScholarshipAwardResponse detail(UUID org, UUID id) {
    return ScholarshipAwardResponse.from(require(org, id));
  }

  @Transactional(readOnly = true)
  public Page<ScholarshipAwardResponse> list(
      UUID org,
      UUID program,
      ScholarshipAwardStatus status,
      LocalDate from,
      LocalDate to,
      Pageable pageable) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Award filter end precedes start");
    Specification<ScholarshipAward> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (program != null) s = s.and((r, q, c) -> c.equal(r.get("program").get("id"), program));
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (from != null) s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("awardDate"), from));
    if (to != null) s = s.and((r, q, c) -> c.lessThanOrEqualTo(r.get("awardDate"), to));
    return awards.findAll(s, pageable).map(ScholarshipAwardResponse::from);
  }

  private static String clean(String x) {
    return x == null || x.isBlank() ? null : x.trim();
  }
}
