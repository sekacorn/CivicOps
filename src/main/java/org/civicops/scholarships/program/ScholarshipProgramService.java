package org.civicops.scholarships.program;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.UserService;
import org.civicops.scholarships.program.dto.*;
import org.civicops.shared.exception.*;
import org.civicops.shared.finance.Money;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScholarshipProgramService {
  private final ScholarshipProgramRepository programs;
  private final OrganizationService organizations;
  private final UserService users;

  public ScholarshipProgramService(
      ScholarshipProgramRepository p, OrganizationService o, UserService u) {
    programs = p;
    organizations = o;
    users = u;
  }

  @Transactional
  public ScholarshipProgramResponse create(
      UUID org, UUID actor, CreateScholarshipProgramRequest r) {
    BigDecimal amount = r.awardAmount() == null ? null : Money.amount(r.awardAmount());
    return ScholarshipProgramResponse.from(
        programs.save(
            new ScholarshipProgram(
                organizations.requireEntity(org),
                users.requireEntity(actor),
                r.name().trim(),
                clean(r.description()),
                clean(r.academicYear()),
                r.applicationOpenDate(),
                r.applicationDeadline(),
                amount,
                r.numberOfAwards(),
                clean(r.eligibilityDescription()))));
  }

  @Transactional(readOnly = true)
  public ScholarshipProgram require(UUID org, UUID id) {
    return programs
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Scholarship program", id));
  }

  @Transactional
  public ScholarshipProgram requireLocked(UUID org, UUID id) {
    return programs
        .findLocked(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Scholarship program", id));
  }

  @Transactional(readOnly = true)
  public ScholarshipProgramResponse detail(UUID org, UUID id) {
    return ScholarshipProgramResponse.from(require(org, id));
  }

  @Transactional
  public ScholarshipProgramResponse update(UUID org, UUID id, UpdateScholarshipProgramRequest r) {
    ScholarshipProgram p = require(org, id);
    p.update(
        clean(r.name()),
        clean(r.description()),
        clean(r.academicYear()),
        r.applicationOpenDate(),
        r.applicationDeadline(),
        r.awardAmount() == null ? null : Money.amount(r.awardAmount()),
        r.numberOfAwards(),
        clean(r.eligibilityDescription()));
    return ScholarshipProgramResponse.from(p);
  }

  @Transactional
  public ScholarshipProgramResponse transition(UUID org, UUID id, ScholarshipProgramStatus target) {
    ScholarshipProgram p = require(org, id);
    p.transition(target);
    return ScholarshipProgramResponse.from(p);
  }

  @Transactional(readOnly = true)
  public Page<ScholarshipProgramResponse> list(
      UUID org,
      ScholarshipProgramStatus status,
      String year,
      LocalDate openFrom,
      LocalDate openTo,
      Pageable pageable) {
    if (openFrom != null && openTo != null && openTo.isBefore(openFrom))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Program filter end precedes start");
    Specification<ScholarshipProgram> s =
        (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (year != null) s = s.and((r, q, c) -> c.equal(r.get("academicYear"), year));
    if (openFrom != null)
      s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("applicationOpenDate"), openFrom));
    if (openTo != null)
      s = s.and((r, q, c) -> c.lessThanOrEqualTo(r.get("applicationOpenDate"), openTo));
    return programs.findAll(s, pageable).map(ScholarshipProgramResponse::from);
  }

  private static String clean(String x) {
    return x == null || x.isBlank() ? null : x.trim();
  }
}
