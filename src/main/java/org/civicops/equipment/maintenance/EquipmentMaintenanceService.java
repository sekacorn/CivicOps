package org.civicops.equipment.maintenance;

import java.time.*;
import java.util.*;
import org.civicops.core.user.UserService;
import org.civicops.equipment.asset.*;
import org.civicops.equipment.maintenance.dto.*;
import org.civicops.shared.exception.*;
import org.civicops.shared.finance.Money;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentMaintenanceService {
  private final EquipmentMaintenanceRepository records;
  private final EquipmentAssetService assets;
  private final UserService users;
  private final Clock clock;

  public EquipmentMaintenanceService(
      EquipmentMaintenanceRepository r, EquipmentAssetService a, UserService u, Clock c) {
    records = r;
    assets = a;
    users = u;
    clock = c;
  }

  @Transactional
  public EquipmentMaintenanceResponse create(
      UUID org, UUID assetId, UUID creator, CreateMaintenanceRequest r) {
    EquipmentAsset a = assets.requireLocked(org, assetId);
    a.beginMaintenance();
    Instant started = r.startedAt() == null ? Instant.now(clock) : r.startedAt();
    EquipmentMaintenanceRecord x =
        new EquipmentMaintenanceRecord(
            a.getOrganization(),
            a,
            r.maintenanceType(),
            r.description().trim(),
            started,
            r.cost() == null ? null : Money.amount(r.cost()),
            clean(r.vendor()),
            users.requireEntity(creator),
            clean(r.notes()));
    return EquipmentMaintenanceResponse.from(records.save(x));
  }

  @Transactional(readOnly = true)
  public EquipmentMaintenanceRecord require(UUID org, UUID id) {
    return records
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Equipment maintenance", id));
  }

  @Transactional(readOnly = true)
  public EquipmentMaintenanceResponse detail(UUID org, UUID id) {
    return EquipmentMaintenanceResponse.from(require(org, id));
  }

  @Transactional
  public EquipmentMaintenanceResponse start(UUID org, UUID id) {
    EquipmentMaintenanceRecord r = require(org, id);
    assets.requireLocked(org, r.getEquipmentAsset().getId());
    r.start();
    return EquipmentMaintenanceResponse.from(r);
  }

  @Transactional
  public EquipmentMaintenanceResponse complete(
      UUID org, UUID id, UUID user, CompleteMaintenanceRequest request) {
    EquipmentMaintenanceRecord r = require(org, id);
    EquipmentAsset a = assets.requireLocked(org, r.getEquipmentAsset().getId());
    r.complete(Instant.now(clock), users.requireEntity(user));
    a.completeMaintenance(request.resultingCondition());
    return EquipmentMaintenanceResponse.from(r);
  }

  @Transactional
  public EquipmentMaintenanceResponse cancel(UUID org, UUID id) {
    EquipmentMaintenanceRecord r = require(org, id);
    EquipmentAsset a = assets.requireLocked(org, r.getEquipmentAsset().getId());
    r.cancel();
    a.cancelMaintenance();
    return EquipmentMaintenanceResponse.from(r);
  }

  @Transactional(readOnly = true)
  public Page<EquipmentMaintenanceResponse> list(
      UUID org,
      UUID asset,
      MaintenanceStatus status,
      MaintenanceType type,
      Instant from,
      Instant to,
      Pageable p) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Filter end must not precede start");
    Specification<EquipmentMaintenanceRecord> s =
        (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (asset != null) s = s.and((r, q, c) -> c.equal(r.get("equipmentAsset").get("id"), asset));
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (type != null) s = s.and((r, q, c) -> c.equal(r.get("maintenanceType"), type));
    if (from != null) s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("startedAt"), from));
    if (to != null) s = s.and((r, q, c) -> c.lessThanOrEqualTo(r.get("startedAt"), to));
    return records.findAll(s, p).map(EquipmentMaintenanceResponse::from);
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
