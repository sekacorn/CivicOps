package org.civicops.equipment.reporting;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.civicops.equipment.asset.*;
import org.civicops.equipment.checkout.*;
import org.civicops.equipment.maintenance.*;
import org.civicops.equipment.reporting.dto.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.civicops.shared.finance.Money;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentReportingService {
  private final EquipmentAssetRepository assets;
  private final EquipmentCheckoutRepository checkouts;
  private final EquipmentMaintenanceRepository maintenance;
  private final Clock clock;

  public EquipmentReportingService(
      EquipmentAssetRepository a,
      EquipmentCheckoutRepository c,
      EquipmentMaintenanceRepository m,
      Clock clock) {
    assets = a;
    checkouts = c;
    maintenance = m;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public EquipmentInventorySummaryResponse summary(UUID org) {
    Instant now = Instant.now(clock);
    return new EquipmentInventorySummaryResponse(
        org,
        assets.countByOrganizationId(org),
        count(org, AssetStatus.AVAILABLE),
        count(org, AssetStatus.CHECKED_OUT),
        count(org, AssetStatus.MAINTENANCE),
        count(org, AssetStatus.LOST),
        count(org, AssetStatus.RETIRED),
        checkouts.countByOrganizationIdAndStatusAndDueAtBefore(org, CheckoutStatus.ACTIVE, now));
  }

  @Transactional(readOnly = true)
  public EquipmentUtilizationResponse utilization(UUID org, LocalDate from, LocalDate to) {
    dates(from, to);
    Instant start = from == null ? Instant.EPOCH : from.atStartOfDay(ZoneOffset.UTC).toInstant(),
        end =
            to == null
                ? Instant.now(clock)
                : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Specification<EquipmentCheckout> s =
        (r, q, c) ->
            c.and(
                c.equal(r.get("organization").get("id"), org),
                c.greaterThanOrEqualTo(r.get("checkedOutAt"), start),
                c.lessThan(r.get("checkedOutAt"), end));
    List<EquipmentCheckout> rows = checkouts.findAll(s);
    List<EquipmentUtilizationResponse.AssetCheckoutCount> frequent =
        rows.stream()
            .collect(
                Collectors.groupingBy(EquipmentCheckout::getEquipmentAsset, Collectors.counting()))
            .entrySet()
            .stream()
            .map(
                e ->
                    new EquipmentUtilizationResponse.AssetCheckoutCount(
                        e.getKey().getId(),
                        e.getKey().getAssetTag(),
                        e.getKey().getName(),
                        e.getValue()))
            .sorted(
                Comparator.comparingLong(
                        EquipmentUtilizationResponse.AssetCheckoutCount::checkoutCount)
                    .reversed())
            .limit(10)
            .toList();
    return new EquipmentUtilizationResponse(
        org,
        from,
        to,
        rows.size(),
        frequent,
        checkouts.countByOrganizationIdAndStatusAndDueAtBefore(
            org, CheckoutStatus.ACTIVE, Instant.now(clock)),
        count(org, AssetStatus.MAINTENANCE));
  }

  @Transactional(readOnly = true)
  public EquipmentMaintenanceReportResponse maintenance(UUID org, LocalDate from, LocalDate to) {
    dates(from, to);
    Specification<EquipmentMaintenanceRecord> s =
        (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (from != null) {
      Instant x = from.atStartOfDay(ZoneOffset.UTC).toInstant();
      s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("startedAt"), x));
    }
    if (to != null) {
      Instant x = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
      s = s.and((r, q, c) -> c.lessThan(r.get("startedAt"), x));
    }
    List<EquipmentMaintenanceRecord> rows = maintenance.findAll(s);
    Map<MaintenanceType, Long> types =
        rows.stream()
            .collect(
                Collectors.groupingBy(
                    EquipmentMaintenanceRecord::getMaintenanceType,
                    () -> new EnumMap<>(MaintenanceType.class),
                    Collectors.counting()));
    BigDecimal total =
        rows.stream()
            .map(EquipmentMaintenanceRecord::getCost)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new EquipmentMaintenanceReportResponse(
        org,
        from,
        to,
        maintenance.countByOrganizationIdAndStatusIn(
            org, List.of(MaintenanceStatus.OPEN, MaintenanceStatus.IN_PROGRESS)),
        types,
        Money.amount(total));
  }

  private long count(UUID org, AssetStatus s) {
    return assets.countByOrganizationIdAndStatus(org, s);
  }

  private static void dates(LocalDate from, LocalDate to) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Report end must not precede start");
  }
}
