package org.civicops.equipment.category;

import java.util.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.equipment.category.dto.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentCategoryService {
  private final EquipmentCategoryRepository categories;
  private final OrganizationService organizations;

  public EquipmentCategoryService(EquipmentCategoryRepository c, OrganizationService o) {
    categories = c;
    organizations = o;
  }

  @Transactional
  public EquipmentCategoryResponse create(UUID org, CreateEquipmentCategoryRequest r) {
    String name = r.name().trim(), normalized = normalize(name);
    if (categories.existsByOrganizationIdAndNormalizedName(org, normalized))
      throw new ConflictException(
          "DUPLICATE_EQUIPMENT_CATEGORY", "Category name already exists in this organization");
    return EquipmentCategoryResponse.from(
        categories.save(
            new EquipmentCategory(
                organizations.requireEntity(org), name, normalized, clean(r.description()))));
  }

  @Transactional(readOnly = true)
  public EquipmentCategory require(UUID org, UUID id) {
    return categories
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Equipment category", id));
  }

  @Transactional(readOnly = true)
  public Page<EquipmentCategoryResponse> list(UUID org, Pageable p) {
    return categories.findAllByOrganizationId(org, p).map(EquipmentCategoryResponse::from);
  }

  @Transactional
  public EquipmentCategoryResponse update(UUID org, UUID id, UpdateEquipmentCategoryRequest r) {
    EquipmentCategory c = require(org, id);
    String name = clean(r.name()), normalized = name == null ? null : normalize(name);
    if (normalized != null
        && !normalized.equals(c.getNormalizedName())
        && categories.existsByOrganizationIdAndNormalizedName(org, normalized))
      throw new ConflictException(
          "DUPLICATE_EQUIPMENT_CATEGORY", "Category name already exists in this organization");
    c.update(name, normalized, clean(r.description()), r.active());
    return EquipmentCategoryResponse.from(c);
  }

  public static String normalize(String s) {
    return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
