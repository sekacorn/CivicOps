package org.civicops.cases.reporting;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.civicops.cases.casefile.*;
import org.civicops.cases.client.ClientRepository;
import org.civicops.cases.reporting.dto.*;
import org.civicops.cases.service.*;
import org.civicops.cases.task.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.civicops.shared.finance.Money;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseReportingService {
  private final ClientRepository clients;
  private final CaseRecordRepository cases;
  private final CaseTaskRepository tasks;
  private final CaseServiceRecordRepository services;
  private final Clock clock;

  public CaseReportingService(
      ClientRepository c,
      CaseRecordRepository r,
      CaseTaskRepository t,
      CaseServiceRecordRepository s,
      Clock clock) {
    clients = c;
    cases = r;
    tasks = t;
    services = s;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public CaseReportSummaryResponse summary(UUID org, LocalDate from, LocalDate to) {
    dates(from, to);
    LocalDate start = from == null ? LocalDate.of(1900, 1, 1) : from,
        end = to == null ? LocalDate.now(clock) : to;
    return new CaseReportSummaryResponse(
        org,
        from,
        to,
        clients.countByOrganizationIdAndActiveTrue(org),
        cases.countByOrganizationIdAndStatus(org, CaseStatus.OPEN),
        cases.countByOrganizationIdAndStatus(org, CaseStatus.IN_PROGRESS),
        cases.countByOrganizationIdAndStatus(org, CaseStatus.ON_HOLD),
        cases.countByOrganizationIdAndStatusAndClosedDateBetween(
            org, CaseStatus.CLOSED, start, end),
        tasks.countByOrganizationIdAndDueDateBeforeAndStatusNotIn(
            org,
            LocalDate.now(clock),
            List.of(CaseTaskStatus.COMPLETED, CaseTaskStatus.CANCELLED)));
  }

  @Transactional(readOnly = true)
  public List<CaseWorkloadResponse> workload(UUID org) {
    Specification<CaseRecord> s =
        (r, q, c) ->
            c.and(
                c.equal(r.get("organization").get("id"), org),
                c.isNotNull(r.get("assignedUser")),
                r.get("status").in(CaseStatus.OPEN, CaseStatus.IN_PROGRESS, CaseStatus.ON_HOLD));
    return cases.findAll(s).stream()
        .collect(Collectors.groupingBy(CaseRecord::getAssignedUser))
        .entrySet()
        .stream()
        .map(
            e ->
                new CaseWorkloadResponse(
                    e.getKey().getId(),
                    (e.getKey().getFirstName() + " " + e.getKey().getLastName()).trim(),
                    count(e.getValue(), CaseStatus.OPEN),
                    count(e.getValue(), CaseStatus.IN_PROGRESS),
                    count(e.getValue(), CaseStatus.ON_HOLD)))
        .sorted(Comparator.comparing(CaseWorkloadResponse::displayName))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CaseServiceTypeReportResponse> services(UUID org, LocalDate from, LocalDate to) {
    dates(from, to);
    Specification<CaseServiceRecord> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (from != null) s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("serviceDate"), from));
    if (to != null) s = s.and((r, q, c) -> c.lessThanOrEqualTo(r.get("serviceDate"), to));
    return services.findAll(s).stream()
        .collect(Collectors.groupingBy(CaseServiceRecord::getServiceType))
        .entrySet()
        .stream()
        .map(
            e ->
                new CaseServiceTypeReportResponse(
                    e.getKey(),
                    e.getValue().size(),
                    Money.amount(
                        e.getValue().stream()
                            .map(CaseServiceRecord::getValueAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add))))
        .sorted(Comparator.comparing(x -> x.serviceType().name()))
        .toList();
  }

  private static long count(List<CaseRecord> x, CaseStatus s) {
    return x.stream().filter(c -> c.getStatus() == s).count();
  }

  private static void dates(LocalDate from, LocalDate to) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Report end must not precede start");
  }
}
