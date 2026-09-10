package org.civicops.foodpantry.distribution;

import java.math.*;
import java.time.*;
import java.util.*;
import org.civicops.core.user.UserService;
import org.civicops.foodpantry.distribution.dto.*;
import org.civicops.foodpantry.household.*;
import org.civicops.foodpantry.inventory.*;
import org.civicops.foodpantry.item.*;
import org.civicops.foodpantry.pantry.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PantryDistributionService {
  private final PantryDistributionVisitRepository visits;
  private final PantryDistributionItemRepository lines;
  private final FoodPantryLocationService pantries;
  private final PantryHouseholdService households;
  private final PantryItemService items;
  private final PantryInventoryLotRepository lots;
  private final PantryInventoryTransactionRepository transactions;
  private final UserService users;
  private final Clock clock;

  public PantryDistributionService(
      PantryDistributionVisitRepository v,
      PantryDistributionItemRepository l,
      FoodPantryLocationService p,
      PantryHouseholdService h,
      PantryItemService i,
      PantryInventoryLotRepository lots,
      PantryInventoryTransactionRepository t,
      UserService u,
      Clock c) {
    visits = v;
    lines = l;
    pantries = p;
    households = h;
    items = i;
    this.lots = lots;
    transactions = t;
    users = u;
    clock = c;
  }

  @Transactional
  public DistributionVisitDetailResponse create(
      UUID org, UUID pantryId, UUID actor, CreateDistributionVisitRequest r) {
    FoodPantryLocation pantry = pantries.require(org, pantryId);
    if (!pantry.isActive())
      throw new BusinessRuleException("INACTIVE_PANTRY", "Pantry must be active");
    PantryHousehold household =
        r.householdId() == null ? null : households.require(org, r.householdId());
    if (household != null && !household.isActive())
      throw new BusinessRuleException("INACTIVE_HOUSEHOLD", "Household must be active");
    int size =
        r.householdSizeAtVisit() != null
            ? r.householdSizeAtVisit()
            : household == null ? 0 : household.getHouseholdSize();
    PantryDistributionVisit visit =
        visits.save(
            new PantryDistributionVisit(
                pantry,
                household,
                r.recipientName(),
                r.visitDateTime(),
                size,
                r.notes(),
                users.requireEntity(actor)));
    return detailOf(visit);
  }

  @Transactional
  public DistributionVisitDetailResponse addItem(
      UUID org, UUID visitId, AddDistributionItemRequest r) {
    PantryDistributionVisit visit = require(org, visitId);
    if (visit.getStatus() != DistributionVisitStatus.OPEN)
      throw new BusinessRuleException("VISIT_NOT_OPEN", "Items may only be added to an open visit");
    PantryItem item = items.require(org, r.itemId());
    if (!item.isActive())
      throw new BusinessRuleException("INACTIVE_PANTRY_ITEM", "Item must be active");
    if (lines.existsByDistributionVisitIdAndPantryItemId(visitId, item.getId()))
      throw new ConflictException("DUPLICATE_DISTRIBUTION_ITEM", "Item already exists on visit");
    lines.save(new PantryDistributionItem(visit, item, r.quantity()));
    return detailOf(visit);
  }

  @Transactional
  public DistributionVisitDetailResponse complete(UUID org, UUID visitId, UUID actor) {
    PantryDistributionVisit visit =
        visits
            .findLocked(org, visitId)
            .orElseThrow(() -> new ResourceNotFoundException("Pantry distribution visit", visitId));
    if (visit.getStatus() != DistributionVisitStatus.OPEN)
      throw new BusinessRuleException("VISIT_NOT_OPEN", "Only open visits can be completed");
    List<PantryDistributionItem> requested =
        lines.findAllByDistributionVisitIdOrderByPantryItemId(visitId);
    if (requested.isEmpty())
      throw new BusinessRuleException("EMPTY_DISTRIBUTION", "At least one item is required");
    LocalDate today =
        LocalDate.now(clock.withZone(ZoneId.of(visit.getPantryLocation().getTimezone())));
    for (PantryDistributionItem line : requested) {
      List<PantryInventoryLot> available =
          lots.findAvailableForUpdate(
              org, visit.getPantryLocation().getId(), line.getPantryItem().getId(), today);
      BigDecimal total =
          available.stream()
              .map(PantryInventoryLot::getQuantityRemaining)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      if (total.compareTo(line.getQuantity()) < 0)
        throw new BusinessRuleException(
            "INSUFFICIENT_INVENTORY",
            "Insufficient non-expired inventory for " + line.getPantryItem().getName());
      BigDecimal remaining = line.getQuantity();
      for (PantryInventoryLot lot : available) {
        if (remaining.signum() == 0) break;
        BigDecimal used = lot.getQuantityRemaining().min(remaining);
        lot.consume(used);
        transactions.save(
            new PantryInventoryTransaction(
                lot,
                InventoryTransactionType.DISTRIBUTION,
                used,
                Instant.now(clock),
                "DISTRIBUTION_VISIT",
                visitId,
                null,
                users.requireEntity(actor)));
        remaining = remaining.subtract(used);
      }
    }
    visit.complete(Instant.now(clock));
    return detailOf(visit);
  }

  @Transactional
  public DistributionVisitDetailResponse cancel(UUID org, UUID visitId) {
    PantryDistributionVisit v = require(org, visitId);
    v.cancel(Instant.now(clock));
    return detailOf(v);
  }

  @Transactional(readOnly = true)
  public PantryDistributionVisit require(UUID org, UUID id) {
    return visits
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Pantry distribution visit", id));
  }

  @Transactional(readOnly = true)
  public DistributionVisitDetailResponse detail(UUID org, UUID id) {
    return detailOf(require(org, id));
  }

  @Transactional(readOnly = true)
  public Page<DistributionVisitSummaryResponse> list(
      UUID org,
      UUID pantry,
      UUID household,
      DistributionVisitStatus status,
      Instant from,
      Instant to,
      Pageable page) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Visit end precedes start");
    Specification<PantryDistributionVisit> s = scope(org, pantry, household, status, from, to);
    return visits.findAll(s, page).map(DistributionVisitSummaryResponse::from);
  }

  @Transactional(readOnly = true)
  public List<DistributionVisitSummaryResponse> householdVisits(UUID org, UUID household) {
    households.require(org, household);
    return visits
        .findAll(
            scope(org, null, household, null, null, null),
            Sort.by(Sort.Direction.DESC, "visitDateTime"))
        .stream()
        .map(DistributionVisitSummaryResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PantryDistributionVisit> completed(UUID org, UUID pantry, Instant from, Instant to) {
    return visits.findAll(scope(org, pantry, null, DistributionVisitStatus.COMPLETED, from, to));
  }

  private DistributionVisitDetailResponse detailOf(PantryDistributionVisit v) {
    return DistributionVisitDetailResponse.from(
        v, lines.findAllByDistributionVisitIdOrderByPantryItemId(v.getId()));
  }

  private static Specification<PantryDistributionVisit> scope(
      UUID org,
      UUID pantry,
      UUID household,
      DistributionVisitStatus status,
      Instant from,
      Instant to) {
    Specification<PantryDistributionVisit> s =
        (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (pantry != null) s = s.and((r, q, c) -> c.equal(r.get("pantryLocation").get("id"), pantry));
    if (household != null) s = s.and((r, q, c) -> c.equal(r.get("household").get("id"), household));
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (from != null) s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("visitDateTime"), from));
    if (to != null) s = s.and((r, q, c) -> c.lessThan(r.get("visitDateTime"), to));
    return s;
  }
}
