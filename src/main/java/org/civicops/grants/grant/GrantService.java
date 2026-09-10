package org.civicops.grants.grant;

import java.math.*;
import java.time.LocalDate;
import java.util.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.*;
import org.civicops.grants.expense.GrantExpenseRepository;
import org.civicops.grants.grant.dto.*;
import org.civicops.shared.exception.*;
import org.civicops.shared.finance.Money;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrantService {
  private final GrantRepository grants;
  private final GrantExpenseRepository expenses;
  private final OrganizationService organizations;
  private final UserService users;

  public GrantService(
      GrantRepository grants,
      GrantExpenseRepository expenses,
      OrganizationService organizations,
      UserService users) {
    this.grants = grants;
    this.expenses = expenses;
    this.organizations = organizations;
    this.users = users;
  }

  @Transactional
  public GrantDetailResponse create(UUID organizationId, UUID userId, CreateGrantRequest request) {
    if (request.submittedDate() != null || request.awardDate() != null) {
      throw new BusinessRuleException(
          "LIFECYCLE_DATE_NOT_ALLOWED",
          "Submission and award dates are assigned by lifecycle transitions");
    }
    validate(
        request.startDate(),
        request.endDate(),
        request.reportingDeadline(),
        request.restricted(),
        request.restrictionDescription());
    Grant grant =
        new Grant(
            organizations.requireEntity(organizationId),
            users.requireEntity(userId),
            request.grantName().trim(),
            request.grantorName().trim(),
            clean(request.grantNumber()),
            clean(request.description()),
            money(request.awardAmount()),
            request.applicationDeadline(),
            null,
            null,
            request.startDate(),
            request.endDate(),
            request.reportingDeadline(),
            request.restricted(),
            clean(request.restrictionDescription()),
            clean(request.primaryContactName()),
            normalizeEmail(request.primaryContactEmail()),
            clean(request.notes()));
    return GrantDetailResponse.from(grants.save(grant));
  }

  @Transactional(readOnly = true)
  public Grant require(UUID organizationId, UUID grantId) {
    return grants
        .findByIdAndOrganizationId(grantId, organizationId)
        .orElseThrow(() -> new ResourceNotFoundException("Grant", grantId));
  }

  @Transactional(readOnly = true)
  public GrantDetailResponse detail(UUID organizationId, UUID grantId) {
    return GrantDetailResponse.from(require(organizationId, grantId));
  }

  @Transactional(readOnly = true)
  public Page<GrantSummaryResponse> list(
      UUID organizationId,
      GrantStatus status,
      String grantor,
      LocalDate reportingDeadlineFrom,
      LocalDate reportingDeadlineTo,
      LocalDate startFrom,
      LocalDate endBefore,
      Boolean restricted,
      Pageable pageable) {
    range(reportingDeadlineFrom, reportingDeadlineTo, "reporting deadline");
    Specification<Grant> spec =
        (root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId);
    if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    if (grantor != null && !grantor.isBlank()) {
      String pattern = "%" + grantor.trim().toLowerCase(Locale.ROOT) + "%";
      spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("grantorName")), pattern));
    }
    if (reportingDeadlineFrom != null)
      spec =
          spec.and(
              (r, q, cb) ->
                  cb.greaterThanOrEqualTo(r.get("reportingDeadline"), reportingDeadlineFrom));
    if (reportingDeadlineTo != null)
      spec =
          spec.and(
              (r, q, cb) -> cb.lessThanOrEqualTo(r.get("reportingDeadline"), reportingDeadlineTo));
    if (startFrom != null)
      spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("startDate"), startFrom));
    if (endBefore != null)
      spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("endDate"), endBefore));
    if (restricted != null)
      spec = spec.and((r, q, cb) -> cb.equal(r.get("restricted"), restricted));
    return grants.findAll(spec, pageable).map(GrantSummaryResponse::from);
  }

  @Transactional
  public GrantDetailResponse update(UUID organizationId, UUID grantId, UpdateGrantRequest request) {
    Grant grant = locked(organizationId, grantId);
    BigDecimal award =
        request.awardAmount() == null ? grant.getAwardAmount() : money(request.awardAmount());
    LocalDate start = request.startDate() == null ? grant.getStartDate() : request.startDate();
    LocalDate end = request.endDate() == null ? grant.getEndDate() : request.endDate();
    LocalDate report =
        request.reportingDeadline() == null
            ? grant.getReportingDeadline()
            : request.reportingDeadline();
    boolean restricted = request.restricted() == null ? grant.isRestricted() : request.restricted();
    String restriction =
        request.restrictionDescription() == null
            ? grant.getRestrictionDescription()
            : request.restrictionDescription();
    validate(start, end, report, restricted, restriction);
    if (award.compareTo(expenses.totalForGrant(grantId)) < 0) {
      throw new BusinessRuleException(
          "AWARD_BELOW_RECORDED_EXPENSES",
          "Award amount cannot be reduced below recorded grant expenditure");
    }
    grant.update(
        trim(request.grantName()),
        trim(request.grantorName()),
        clean(request.grantNumber()),
        clean(request.description()),
        request.awardAmount() == null ? null : award,
        request.applicationDeadline(),
        request.startDate(),
        request.endDate(),
        request.reportingDeadline(),
        request.restricted(),
        clean(request.restrictionDescription()),
        clean(request.primaryContactName()),
        normalizeEmail(request.primaryContactEmail()),
        clean(request.notes()));
    return GrantDetailResponse.from(grant);
  }

  @Transactional
  public GrantDetailResponse transition(UUID organizationId, UUID grantId, GrantStatus target) {
    Grant grant = locked(organizationId, grantId);
    grant.transition(target, LocalDate.now());
    return GrantDetailResponse.from(grant);
  }

  private Grant locked(UUID organizationId, UUID grantId) {
    return grants
        .findLockedByIdAndOrganizationId(grantId, organizationId)
        .orElseThrow(() -> new ResourceNotFoundException("Grant", grantId));
  }

  public static BigDecimal money(BigDecimal amount) {
    return Money.amount(amount);
  }

  private static void validate(
      LocalDate start,
      LocalDate end,
      LocalDate report,
      boolean restricted,
      String restrictionDescription) {
    if (start != null && end != null && end.isBefore(start))
      throw new BusinessRuleException(
          "INVALID_GRANT_DATE_RANGE", "Grant end date cannot precede start date");
    if (report != null && end != null && report.isBefore(end))
      throw new BusinessRuleException(
          "INVALID_REPORTING_DEADLINE", "Reporting deadline cannot precede grant end date");
    if (restricted && (restrictionDescription == null || restrictionDescription.isBlank()))
      throw new BusinessRuleException(
          "RESTRICTION_DESCRIPTION_REQUIRED",
          "Restriction description is required for a restricted grant");
  }

  private static void range(LocalDate from, LocalDate to, String label) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Invalid " + label + " filter range");
  }

  private static String trim(String value) {
    return value == null ? null : value.trim();
  }

  private static String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String normalizeEmail(String value) {
    return value == null || value.isBlank() ? null : UserService.normalizeEmail(value);
  }
}
