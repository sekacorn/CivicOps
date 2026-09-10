package org.civicops.equipment.checkout;

import java.time.*;
import java.util.*;
import org.civicops.core.membership.OrganizationMembershipRepository;
import org.civicops.core.user.*;
import org.civicops.equipment.asset.*;
import org.civicops.equipment.checkout.dto.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentCheckoutService {
  private final EquipmentCheckoutRepository checkouts;
  private final EquipmentAssetService assets;
  private final UserService users;
  private final OrganizationMembershipRepository memberships;
  private final Clock clock;

  public EquipmentCheckoutService(
      EquipmentCheckoutRepository c,
      EquipmentAssetService a,
      UserService u,
      OrganizationMembershipRepository m,
      Clock clock) {
    checkouts = c;
    assets = a;
    users = u;
    memberships = m;
    this.clock = clock;
  }

  @Transactional
  public EquipmentCheckoutDetailResponse checkout(
      UUID org, UUID assetId, UUID issuerId, CreateEquipmentCheckoutRequest r) {
    Instant now = Instant.now(clock);
    if (!r.dueAt().isAfter(now))
      throw new BusinessRuleException(
          "INVALID_CHECKOUT_DUE_DATE", "Checkout due time must be after checkout time");
    User borrower = null;
    String borrowerName = clean(r.borrowerName()), borrowerEmail = email(r.borrowerEmail());
    if (r.borrowerUserId() != null) borrower = member(org, r.borrowerUserId(), "borrower");
    else if (borrowerName == null)
      throw new BusinessRuleException("BORROWER_REQUIRED", "External borrower name is required");
    User issuer = member(org, issuerId, "issuer");
    EquipmentAsset asset = assets.requireLocked(org, assetId);
    asset.checkout();
    EquipmentCheckout checkout =
        new EquipmentCheckout(
            asset.getOrganization(),
            asset,
            borrower,
            borrowerName,
            borrowerEmail,
            now,
            r.dueAt(),
            r.checkoutCondition(),
            clean(r.notes()),
            issuer);
    return EquipmentCheckoutDetailResponse.from(checkouts.save(checkout), now);
  }

  @Transactional(readOnly = true)
  public EquipmentCheckout require(UUID org, UUID id) {
    return checkouts
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Equipment checkout", id));
  }

  @Transactional(readOnly = true)
  public EquipmentCheckoutDetailResponse detail(UUID org, UUID id) {
    return EquipmentCheckoutDetailResponse.from(require(org, id), Instant.now(clock));
  }

  @Transactional
  public EquipmentCheckoutDetailResponse checkIn(
      UUID org, UUID id, UUID receiverId, CheckInEquipmentRequest r) {
    EquipmentCheckout c = require(org, id);
    EquipmentAsset asset = assets.requireLocked(org, c.getEquipmentAsset().getId());
    Instant now = Instant.now(clock);
    c.checkIn(now, r.returnCondition(), member(org, receiverId, "receiver"), clean(r.notes()));
    asset.checkIn(r.returnCondition());
    return EquipmentCheckoutDetailResponse.from(c, now);
  }

  @Transactional
  public EquipmentCheckoutDetailResponse markLost(UUID org, UUID id) {
    EquipmentCheckout c = require(org, id);
    EquipmentAsset asset = assets.requireLocked(org, c.getEquipmentAsset().getId());
    c.markLost();
    asset.markLost();
    return EquipmentCheckoutDetailResponse.from(c, Instant.now(clock));
  }

  @Transactional(readOnly = true)
  public Page<EquipmentCheckoutSummaryResponse> list(
      UUID org,
      UUID assetId,
      CheckoutStatus status,
      UUID borrower,
      Boolean overdue,
      Instant outFrom,
      Instant outTo,
      Instant dueFrom,
      Instant dueTo,
      Pageable p) {
    dates(outFrom, outTo);
    dates(dueFrom, dueTo);
    Specification<EquipmentCheckout> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (assetId != null)
      s = s.and((r, q, c) -> c.equal(r.get("equipmentAsset").get("id"), assetId));
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (borrower != null)
      s = s.and((r, q, c) -> c.equal(r.get("borrowerUser").get("id"), borrower));
    if (outFrom != null)
      s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("checkedOutAt"), outFrom));
    if (outTo != null) s = s.and((r, q, c) -> c.lessThanOrEqualTo(r.get("checkedOutAt"), outTo));
    if (dueFrom != null) s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("dueAt"), dueFrom));
    if (dueTo != null) s = s.and((r, q, c) -> c.lessThanOrEqualTo(r.get("dueAt"), dueTo));
    if (overdue != null) {
      Instant now = Instant.now(clock);
      s =
          Boolean.TRUE.equals(overdue)
              ? s.and(
                  (r, q, c) ->
                      c.and(
                          c.equal(r.get("status"), CheckoutStatus.ACTIVE),
                          c.lessThan(r.get("dueAt"), now)))
              : s.and(
                  (r, q, c) ->
                      c.or(
                          c.notEqual(r.get("status"), CheckoutStatus.ACTIVE),
                          c.greaterThanOrEqualTo(r.get("dueAt"), now)));
    }
    Instant now = Instant.now(clock);
    return checkouts.findAll(s, p).map(c -> EquipmentCheckoutSummaryResponse.from(c, now));
  }

  private User member(UUID org, UUID id, String kind) {
    User u = users.requireEntity(id);
    if (!u.isActive() || memberships.findByOrganizationIdAndUserIdAndActiveTrue(org, id).isEmpty())
      throw new BusinessRuleException(
          "INVALID_CHECKOUT_" + kind.toUpperCase(Locale.ROOT),
          "Checkout " + kind + " must be an active organization member");
    return u;
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  private static String email(String s) {
    return clean(s) == null ? null : UserService.normalizeEmail(s);
  }

  private static void dates(Instant from, Instant to) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Filter end must not precede start");
  }
}
