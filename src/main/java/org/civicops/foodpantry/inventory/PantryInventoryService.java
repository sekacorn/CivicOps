package org.civicops.foodpantry.inventory;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.civicops.core.user.UserService;
import org.civicops.foodpantry.inventory.dto.*;
import org.civicops.foodpantry.item.*;
import org.civicops.foodpantry.pantry.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PantryInventoryService {
  public static final int EXPIRING_SOON_DAYS = 7;
  private final PantryInventoryLotRepository lots;
  private final PantryInventoryTransactionRepository transactions;
  private final FoodPantryLocationService pantries;
  private final PantryItemService items;
  private final PantryItemRepository itemRepository;
  private final UserService users;
  private final Clock clock;

  public PantryInventoryService(
      PantryInventoryLotRepository l,
      PantryInventoryTransactionRepository t,
      FoodPantryLocationService p,
      PantryItemService i,
      PantryItemRepository ir,
      UserService u,
      Clock c) {
    lots = l;
    transactions = t;
    pantries = p;
    items = i;
    itemRepository = ir;
    users = u;
    clock = c;
  }

  @Transactional
  public InventoryLotResponse receive(
      UUID org, UUID pantryId, UUID actor, CreateInventoryReceiptRequest r) {
    FoodPantryLocation p = pantries.require(org, pantryId);
    PantryItem i = items.require(org, r.itemId());
    if (!p.isActive() || !i.isActive())
      throw new BusinessRuleException("INACTIVE_PANTRY_RESOURCE", "Pantry and item must be active");
    PantryInventoryLot lot =
        lots.save(
            new PantryInventoryLot(
                p,
                i,
                r.lotNumber(),
                r.receivedDate(),
                r.expirationDate(),
                r.quantity(),
                r.sourceType(),
                r.sourceReference(),
                r.notes(),
                users.requireEntity(actor)));
    transactions.save(
        new PantryInventoryTransaction(
            lot,
            InventoryTransactionType.RECEIPT,
            lot.getQuantityReceived(),
            Instant.now(clock),
            "INVENTORY_RECEIPT",
            lot.getId(),
            r.notes(),
            users.requireEntity(actor)));
    return InventoryLotResponse.from(lot);
  }

  @Transactional
  public InventoryLotResponse adjust(UUID org, UUID lotId, UUID actor, AdjustInventoryRequest r) {
    PantryInventoryLot lot = require(org, lotId);
    InventoryTransactionType type =
        switch (r.adjustmentType()) {
          case COUNT_INCREASE -> InventoryTransactionType.ADJUSTMENT_INCREASE;
          case COUNT_DECREASE -> InventoryTransactionType.ADJUSTMENT_DECREASE;
          case SPOILAGE -> InventoryTransactionType.SPOILAGE;
          case EXPIRATION -> InventoryTransactionType.EXPIRATION;
        };
    if (r.adjustmentType() == InventoryAdjustmentType.COUNT_INCREASE) lot.increase(r.quantity());
    else lot.decrease(r.quantity());
    transactions.save(
        new PantryInventoryTransaction(
            lot,
            type,
            r.quantity(),
            Instant.now(clock),
            "ADJUSTMENT",
            null,
            r.reason().trim(),
            users.requireEntity(actor)));
    return InventoryLotResponse.from(lot);
  }

  @Transactional(readOnly = true)
  public PantryInventoryLot require(UUID org, UUID id) {
    return lots.findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Pantry inventory lot", id));
  }

  @Transactional(readOnly = true)
  public InventoryLotResponse detail(UUID org, UUID id) {
    return InventoryLotResponse.from(require(org, id));
  }

  @Transactional(readOnly = true)
  public List<InventoryTransactionResponse> history(UUID org, UUID id) {
    require(org, id);
    return transactions
        .findAllByOrganizationIdAndInventoryLotIdOrderByOccurredAtAsc(org, id)
        .stream()
        .map(InventoryTransactionResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public Page<InventoryLotResponse> list(
      UUID org,
      UUID pantry,
      UUID item,
      Boolean expired,
      Boolean expiringSoon,
      Boolean available,
      String lotNumber,
      Pageable page) {
    LocalDate today = localToday(pantries.require(org, pantry));
    Specification<PantryInventoryLot> s = scope(org, pantry, item);
    if (expired != null)
      s =
          s.and(
              expired
                  ? (r, q, c) -> c.lessThan(r.get("expirationDate"), today)
                  : (r, q, c) ->
                      c.or(
                          c.isNull(r.get("expirationDate")),
                          c.greaterThanOrEqualTo(r.get("expirationDate"), today)));
    if (Boolean.TRUE.equals(expiringSoon))
      s =
          s.and(
              (r, q, c) ->
                  c.between(r.get("expirationDate"), today, today.plusDays(EXPIRING_SOON_DAYS)));
    if (available != null)
      s =
          s.and(
              available
                  ? (r, q, c) ->
                      c.and(
                          c.greaterThan(r.get("quantityRemaining"), BigDecimal.ZERO),
                          c.or(
                              c.isNull(r.get("expirationDate")),
                              c.greaterThanOrEqualTo(r.get("expirationDate"), today)))
                  : (r, q, c) -> c.equal(r.get("quantityRemaining"), BigDecimal.ZERO));
    if (lotNumber != null) s = s.and((r, q, c) -> c.equal(r.get("lotNumber"), lotNumber.trim()));
    return lots.findAll(s, page).map(InventoryLotResponse::from);
  }

  @Transactional(readOnly = true)
  public List<InventoryAvailabilityResponse> availability(UUID org, UUID pantryId, UUID itemId) {
    FoodPantryLocation pantry = pantries.require(org, pantryId);
    List<PantryItem> inventoryItems =
        itemId == null
            ? itemRepository.findAll(
                (r, q, c) ->
                    c.and(c.equal(r.get("organization").get("id"), org), c.isTrue(r.get("active"))))
            : List.of(items.require(org, itemId));
    List<PantryInventoryLot> rows = lots.findAll(scope(org, pantryId, itemId));
    LocalDate today = localToday(pantry), soon = today.plusDays(EXPIRING_SOON_DAYS);
    Map<UUID, List<PantryInventoryLot>> byItem =
        rows.stream().collect(Collectors.groupingBy(x -> x.getPantryItem().getId()));
    return inventoryItems.stream()
        .map(
            i -> {
              List<PantryInventoryLot> ls = byItem.getOrDefault(i.getId(), List.of());
              BigDecimal available = sum(ls.stream().filter(l -> !l.isExpired(today)));
              BigDecimal expiring =
                  sum(
                      ls.stream()
                          .filter(
                              l ->
                                  l.getExpirationDate() != null
                                      && !l.getExpirationDate().isBefore(today)
                                      && !l.getExpirationDate().isAfter(soon)));
              BigDecimal expiredQty = sum(ls.stream().filter(l -> l.isExpired(today)));
              BigDecimal threshold = i.getReorderThreshold();
              return new InventoryAvailabilityResponse(
                  pantryId,
                  i.getId(),
                  i.getName(),
                  i.getCategory(),
                  i.getUnitType(),
                  available,
                  expiring,
                  expiredQty,
                  threshold,
                  threshold != null && available.compareTo(threshold) <= 0);
            })
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PantryInventoryLot> allLots(UUID org, UUID pantry) {
    return lots.findAll(scope(org, pantry, null));
  }

  private LocalDate localToday(FoodPantryLocation pantry) {
    return LocalDate.now(clock.withZone(ZoneId.of(pantry.getTimezone())));
  }

  private static Specification<PantryInventoryLot> scope(UUID org, UUID pantry, UUID item) {
    Specification<PantryInventoryLot> s =
        (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (pantry != null) s = s.and((r, q, c) -> c.equal(r.get("pantryLocation").get("id"), pantry));
    if (item != null) s = s.and((r, q, c) -> c.equal(r.get("pantryItem").get("id"), item));
    return s;
  }

  private static BigDecimal sum(Stream<PantryInventoryLot> s) {
    return s.map(PantryInventoryLot::getQuantityRemaining)
        .reduce(BigDecimal.ZERO.setScale(3), BigDecimal::add);
  }

  public PantryInventoryLotRepository lotRepository() {
    return lots;
  }

  public PantryInventoryTransactionRepository transactionRepository() {
    return transactions;
  }
}
