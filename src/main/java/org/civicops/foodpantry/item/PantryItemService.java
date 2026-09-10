package org.civicops.foodpantry.item;

import java.util.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.foodpantry.item.dto.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PantryItemService {
  private final PantryItemRepository items;
  private final OrganizationService organizations;

  public PantryItemService(PantryItemRepository i, OrganizationService o) {
    items = i;
    organizations = o;
  }

  @Transactional
  public PantryItemResponse create(UUID org, CreatePantryItemRequest r) {
    String sku = PantryItem.normalizeSku(r.sku());
    if (sku != null && items.existsByOrganizationIdAndNormalizedSku(org, sku))
      throw new ConflictException(
          "DUPLICATE_PANTRY_SKU", "SKU already exists in this organization");
    return PantryItemResponse.from(
        items.save(
            new PantryItem(
                organizations.requireEntity(org),
                r.sku(),
                r.name(),
                r.description(),
                r.category(),
                r.unitType(),
                r.trackExpiration(),
                r.reorderThreshold(),
                r.notes())));
  }

  @Transactional
  public PantryItemResponse update(UUID org, UUID id, UpdatePantryItemRequest r) {
    PantryItem i = require(org, id);
    String sku = PantryItem.normalizeSku(r.sku());
    if (sku != null
        && !sku.equals(i.getNormalizedSku())
        && items.existsByOrganizationIdAndNormalizedSku(org, sku))
      throw new ConflictException(
          "DUPLICATE_PANTRY_SKU", "SKU already exists in this organization");
    i.update(
        r.sku(),
        r.name(),
        r.description(),
        r.category(),
        r.unitType(),
        r.trackExpiration(),
        r.reorderThreshold(),
        r.notes());
    return PantryItemResponse.from(i);
  }

  @Transactional
  public PantryItemResponse active(UUID org, UUID id, boolean active) {
    PantryItem i = require(org, id);
    if (active) i.activate();
    else i.deactivate();
    return PantryItemResponse.from(i);
  }

  @Transactional(readOnly = true)
  public PantryItem require(UUID org, UUID id) {
    return items
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Pantry item", id));
  }

  @Transactional(readOnly = true)
  public PantryItemResponse detail(UUID org, UUID id) {
    return PantryItemResponse.from(require(org, id));
  }

  @Transactional(readOnly = true)
  public Page<PantryItemResponse> list(
      UUID org, FoodCategory category, Boolean active, String name, String sku, Pageable page) {
    Specification<PantryItem> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (category != null) s = s.and((r, q, c) -> c.equal(r.get("category"), category));
    if (active != null) s = s.and((r, q, c) -> c.equal(r.get("active"), active));
    if (name != null)
      s =
          s.and(
              (r, q, c) ->
                  c.like(c.lower(r.get("name")), "%" + name.trim().toLowerCase(Locale.ROOT) + "%"));
    if (sku != null)
      s = s.and((r, q, c) -> c.equal(r.get("normalizedSku"), PantryItem.normalizeSku(sku)));
    return items.findAll(s, page).map(PantryItemResponse::from);
  }
}
