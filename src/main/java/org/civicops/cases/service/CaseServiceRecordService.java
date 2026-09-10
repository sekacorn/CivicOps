package org.civicops.cases.service;

import java.time.LocalDate;
import java.util.*;
import org.civicops.cases.casefile.*;
import org.civicops.cases.service.dto.*;
import org.civicops.core.membership.OrganizationMembershipRepository;
import org.civicops.core.user.*;
import org.civicops.shared.exception.*;
import org.civicops.shared.finance.Money;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseServiceRecordService {
  private final CaseServiceRecordRepository records;
  private final CaseRecordService cases;
  private final UserService users;
  private final OrganizationMembershipRepository memberships;

  public CaseServiceRecordService(
      CaseServiceRecordRepository r,
      CaseRecordService c,
      UserService u,
      OrganizationMembershipRepository m) {
    records = r;
    cases = c;
    users = u;
    memberships = m;
  }

  @Transactional
  public CaseServiceResponse create(UUID org, UUID caseId, CreateCaseServiceRequest r) {
    CaseRecord c = cases.require(org, caseId);
    if (c.isTerminal())
      throw new BusinessRuleException(
          "TERMINAL_CASE_HISTORY_ONLY", "Services cannot be added to a closed or cancelled case");
    User provider = member(org, r.providedByUserId());
    CaseServiceRecord x =
        new CaseServiceRecord(
            c.getOrganization(),
            c,
            r.serviceType(),
            clean(r.description()),
            r.serviceDate(),
            r.quantity() == null ? null : Money.amount(r.quantity()),
            clean(r.unit()),
            r.valueAmount() == null ? null : Money.amount(r.valueAmount()),
            provider,
            clean(r.notes()));
    return CaseServiceResponse.from(records.save(x));
  }

  @Transactional(readOnly = true)
  public CaseServiceResponse detail(UUID org, UUID caseId, UUID id) {
    cases.require(org, caseId);
    return CaseServiceResponse.from(
        records
            .findByIdAndOrganizationIdAndCaseRecordId(id, org, caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case service record", id)));
  }

  @Transactional(readOnly = true)
  public Page<CaseServiceResponse> list(
      UUID org, UUID caseId, CaseServiceType type, LocalDate from, LocalDate to, Pageable p) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException(
          "INVALID_DATE_RANGE", "Service-date filter end must not precede start");
    cases.require(org, caseId);
    Specification<CaseServiceRecord> s =
        (r, q, c) ->
            c.and(
                c.equal(r.get("organization").get("id"), org),
                c.equal(r.get("caseRecord").get("id"), caseId));
    if (type != null) s = s.and((r, q, c) -> c.equal(r.get("serviceType"), type));
    if (from != null) s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("serviceDate"), from));
    if (to != null) s = s.and((r, q, c) -> c.lessThanOrEqualTo(r.get("serviceDate"), to));
    return records.findAll(s, p).map(CaseServiceResponse::from);
  }

  private User member(UUID org, UUID id) {
    if (id == null) return null;
    User u = users.requireEntity(id);
    if (!u.isActive() || memberships.findByOrganizationIdAndUserIdAndActiveTrue(org, id).isEmpty())
      throw new BusinessRuleException(
          "INVALID_SERVICE_PROVIDER", "Service provider must be an active organization member");
    return u;
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
