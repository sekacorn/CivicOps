package org.civicops.equipment.asset;

import java.util.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.equipment.asset.dto.*;
import org.civicops.equipment.category.*;
import org.civicops.shared.exception.*;
import org.civicops.shared.finance.Money;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentAssetService {
  private final EquipmentAssetRepository assets;
  private final EquipmentCategoryService categories;
  private final OrganizationService organizations;

  public EquipmentAssetService(
      EquipmentAssetRepository a, EquipmentCategoryService c, OrganizationService o) {
    assets = a;
    categories = c;
    organizations = o;
  }

  @Transactional
  public EquipmentAssetDetailResponse create(UUID org, CreateEquipmentAssetRequest r) {
    String tag = r.assetTag().trim();
    if (assets.existsByOrganizationIdAndAssetTag(org, tag))
      throw new ConflictException(
          "DUPLICATE_ASSET_TAG", "Asset tag already exists in this organization");
    EquipmentCategory category = category(org, r.categoryId());
    EquipmentAsset a =
        new EquipmentAsset(
            organizations.requireEntity(org),
            tag,
            r.name().trim(),
            clean(r.description()),
            category,
            clean(r.manufacturer()),
            clean(r.model()),
            clean(r.serialNumber()),
            r.purchaseDate(),
            r.purchaseValue() == null ? null : Money.amount(r.purchaseValue()),
            r.condition(),
            clean(r.location()),
            clean(r.notes()));
    return EquipmentAssetDetailResponse.from(assets.save(a));
  }

  @Transactional(readOnly = true)
  public EquipmentAsset require(UUID org, UUID id) {
    return assets
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Equipment asset", id));
  }

  @Transactional(readOnly = true)
  public EquipmentAssetDetailResponse detail(UUID org, UUID id) {
    return EquipmentAssetDetailResponse.from(require(org, id));
  }

  @Transactional
  public EquipmentAssetDetailResponse update(UUID org, UUID id, UpdateEquipmentAssetRequest r) {
    EquipmentAsset a = require(org, id);
    a.update(
        clean(r.name()),
        clean(r.description()),
        category(org, r.categoryId()),
        clean(r.manufacturer()),
        clean(r.model()),
        clean(r.serialNumber()),
        r.purchaseDate(),
        r.purchaseValue() == null ? null : Money.amount(r.purchaseValue()),
        r.condition(),
        clean(r.location()),
        clean(r.notes()));
    return EquipmentAssetDetailResponse.from(a);
  }

  @Transactional
  public EquipmentAssetDetailResponse retire(UUID org, UUID id) {
    EquipmentAsset a =
        assets
            .findLocked(id, org)
            .orElseThrow(() -> new ResourceNotFoundException("Equipment asset", id));
    a.retire();
    return EquipmentAssetDetailResponse.from(a);
  }

  @Transactional(readOnly = true)
  public Page<EquipmentAssetSummaryResponse> list(
      UUID org,
      AssetStatus status,
      EquipmentCondition condition,
      UUID category,
      String tag,
      String name,
      Boolean available,
      Pageable p) {
    Specification<EquipmentAsset> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (condition != null) s = s.and((r, q, c) -> c.equal(r.get("condition"), condition));
    if (category != null) s = s.and((r, q, c) -> c.equal(r.get("category").get("id"), category));
    if (tag != null && !tag.isBlank())
      s = s.and((r, q, c) -> c.equal(r.get("assetTag"), tag.trim()));
    if (name != null && !name.isBlank()) {
      String n = "%" + name.trim().toLowerCase(Locale.ROOT) + "%";
      s = s.and((r, q, c) -> c.like(c.lower(r.get("name")), n));
    }
    if (available != null)
      s =
          Boolean.TRUE.equals(available)
              ? s.and((r, q, c) -> c.equal(r.get("status"), AssetStatus.AVAILABLE))
              : s.and((r, q, c) -> c.notEqual(r.get("status"), AssetStatus.AVAILABLE));
    return assets.findAll(s, p).map(EquipmentAssetSummaryResponse::from);
  }

  public EquipmentAsset requireLocked(UUID org, UUID id) {
    return assets
        .findLocked(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Equipment asset", id));
  }

  private EquipmentCategory category(UUID org, UUID id) {
    if (id == null) return null;
    EquipmentCategory c = categories.require(org, id);
    if (!c.isActive())
      throw new BusinessRuleException(
          "INACTIVE_EQUIPMENT_CATEGORY", "Inactive category cannot be assigned");
    return c;
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
