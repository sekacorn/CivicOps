package org.civicops.foodpantry.reporting;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.civicops.foodpantry.distribution.*;
import org.civicops.foodpantry.household.PantryHouseholdRepository;
import org.civicops.foodpantry.inventory.*;
import org.civicops.foodpantry.inventory.dto.InventoryAvailabilityResponse;
import org.civicops.foodpantry.item.FoodCategory;
import org.civicops.foodpantry.pantry.*;
import org.civicops.foodpantry.reporting.dto.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoodPantryReportingService {
  private final PantryInventoryService inventory;
  private final FoodPantryLocationRepository pantries;
  private final PantryDistributionService distributions;
  private final PantryDistributionItemRepository lines;
  private final PantryHouseholdRepository households;
  private final PantryInventoryTransactionRepository transactions;
  private final Clock clock;

  public FoodPantryReportingService(
      PantryInventoryService i,
      FoodPantryLocationRepository p,
      PantryDistributionService d,
      PantryDistributionItemRepository l,
      PantryHouseholdRepository h,
      PantryInventoryTransactionRepository t,
      Clock c) {
    inventory = i;
    pantries = p;
    distributions = d;
    lines = l;
    households = h;
    transactions = t;
    clock = c;
  }

  @Transactional(readOnly = true)
  public PantryInventorySummaryResponse inventory(UUID org, UUID pantry) {
    List<PantryInventoryLot> lots = inventory.allLots(org, pantry);
    BigDecimal available = sum(lots.stream().filter(l -> !l.isExpired(localToday(l))));
    long expired = lots.stream().filter(l -> l.isExpired(localToday(l))).count(),
        expiring =
            lots.stream()
                .filter(
                    l -> {
                      LocalDate today = localToday(l);
                      return l.getExpirationDate() != null
                          && !l.getExpirationDate().isBefore(today)
                          && !l.getExpirationDate()
                              .isAfter(today.plusDays(PantryInventoryService.EXPIRING_SOON_DAYS));
                    })
                .count();
    List<UUID> pantryIds =
        pantry == null
            ? pantries
                .findAll(
                    (r, q, c) ->
                        c.and(
                            c.equal(r.get("organization").get("id"), org),
                            c.isTrue(r.get("active"))))
                .stream()
                .map(FoodPantryLocation::getId)
                .toList()
            : List.of(pantry);
    List<InventoryAvailabilityResponse> alerts =
        pantryIds.stream().flatMap(id -> inventory.availability(org, id, null).stream()).toList();
    return new PantryInventorySummaryResponse(
        org,
        pantry,
        lots.stream().map(l -> l.getPantryItem().getId()).distinct().count(),
        available,
        alerts.stream().filter(InventoryAvailabilityResponse::lowStock).count(),
        expired,
        expiring);
  }

  @Transactional(readOnly = true)
  public PantryDistributionReportResponse distributions(
      UUID org, UUID pantry, LocalDate from, LocalDate to) {
    dates(from, to);
    Instant start = from == null ? Instant.EPOCH : from.atStartOfDay(ZoneOffset.UTC).toInstant(),
        end =
            to == null
                ? Instant.now(clock).plusSeconds(1)
                : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    List<PantryDistributionVisit> visits = distributions.completed(org, pantry, start, end);
    List<PantryDistributionItem> all =
        visits.stream()
            .flatMap(v -> lines.findAllByDistributionVisitIdOrderByPantryItemId(v.getId()).stream())
            .toList();
    Map<FoodCategory, BigDecimal> categories =
        all.stream()
            .collect(
                Collectors.groupingBy(
                    i -> i.getPantryItem().getCategory(),
                    Collectors.reducing(
                        BigDecimal.ZERO, PantryDistributionItem::getQuantity, BigDecimal::add)));
    List<CategoryDistributionQuantity> byCategory =
        categories.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new CategoryDistributionQuantity(e.getKey(), q(e.getValue())))
            .toList();
    long householdVisits = visits.stream().filter(v -> v.getHousehold() != null).count(),
        unique =
            visits.stream()
                .filter(v -> v.getHousehold() != null)
                .map(v -> v.getHousehold().getId())
                .distinct()
                .count();
    BigDecimal average =
        visits.isEmpty()
            ? BigDecimal.ZERO.setScale(2)
            : BigDecimal.valueOf(
                    visits.stream()
                        .mapToInt(PantryDistributionVisit::getHouseholdSizeAtVisit)
                        .sum())
                .divide(BigDecimal.valueOf(visits.size()), 2, RoundingMode.HALF_UP);
    return new PantryDistributionReportResponse(
        org,
        pantry,
        from,
        to,
        visits.size(),
        householdVisits,
        unique,
        q(
            all.stream()
                .map(PantryDistributionItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add)),
        average,
        byCategory);
  }

  @Transactional(readOnly = true)
  public PantryHouseholdReportResponse households(UUID org, LocalDate from, LocalDate to) {
    dates(from, to);
    Instant start = from == null ? Instant.EPOCH : from.atStartOfDay(ZoneOffset.UTC).toInstant(),
        end =
            to == null
                ? Instant.now(clock).plusSeconds(1)
                : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    List<PantryDistributionVisit> visits =
        distributions.completed(org, null, start, end).stream()
            .filter(v -> v.getHousehold() != null)
            .toList();
    Map<UUID, Long> counts =
        visits.stream()
            .collect(Collectors.groupingBy(v -> v.getHousehold().getId(), Collectors.counting()));
    return new PantryHouseholdReportResponse(
        org,
        from,
        to,
        households.countByOrganizationIdAndActiveTrue(org),
        visits.size(),
        counts.values().stream().filter(x -> x == 1).count(),
        counts.values().stream().filter(x -> x > 1).count());
  }

  @Transactional(readOnly = true)
  public PantryWasteReportResponse waste(UUID org, UUID pantry, LocalDate from, LocalDate to) {
    dates(from, to);
    Instant start = from == null ? Instant.EPOCH : from.atStartOfDay(ZoneOffset.UTC).toInstant(),
        end =
            to == null
                ? Instant.now(clock).plusSeconds(1)
                : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Specification<PantryInventoryTransaction> s =
        (r, q, c) ->
            c.and(
                c.equal(r.get("organization").get("id"), org),
                c.greaterThanOrEqualTo(r.get("occurredAt"), start),
                c.lessThan(r.get("occurredAt"), end));
    if (pantry != null) s = s.and((r, q, c) -> c.equal(r.get("pantryLocation").get("id"), pantry));
    List<PantryInventoryTransaction> rows = transactions.findAll(s);
    return new PantryWasteReportResponse(
        org,
        pantry,
        from,
        to,
        q(
            rows.stream()
                .filter(t -> t.getTransactionType() == InventoryTransactionType.EXPIRATION)
                .map(PantryInventoryTransaction::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add)),
        q(
            rows.stream()
                .filter(t -> t.getTransactionType() == InventoryTransactionType.SPOILAGE)
                .map(PantryInventoryTransaction::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
  }

  private LocalDate localToday(PantryInventoryLot lot) {
    return LocalDate.now(clock.withZone(ZoneId.of(lot.getPantryLocation().getTimezone())));
  }

  private static void dates(LocalDate from, LocalDate to) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Report end precedes start");
  }

  private static BigDecimal sum(Stream<PantryInventoryLot> s) {
    return q(
        s.map(PantryInventoryLot::getQuantityRemaining).reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  private static BigDecimal q(BigDecimal x) {
    return x.setScale(3, RoundingMode.HALF_UP);
  }
}
