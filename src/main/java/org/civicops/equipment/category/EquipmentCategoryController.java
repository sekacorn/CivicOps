package org.civicops.equipment.category;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.equipment.category.dto.*;
import org.civicops.equipment.security.EquipmentAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/equipment-categories")
@Tag(name = "Equipment Categories")
public class EquipmentCategoryController {
  private final EquipmentCategoryService categories;
  private final EquipmentAccessService access;

  public EquipmentCategoryController(EquipmentCategoryService c, EquipmentAccessService a) {
    categories = c;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EquipmentCategoryResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateEquipmentCategoryRequest r) {
    access.requireManageEquipment(organizationId);
    return categories.create(organizationId, r);
  }

  @GetMapping
  public Page<EquipmentCategoryResponse> list(
      @PathVariable UUID organizationId, @PageableDefault(size = 20, sort = "name") Pageable p) {
    access.requireReadEquipment(organizationId);
    return categories.list(
        organizationId, SafePageables.allow(p, Set.of("name", "createdAt", "active")));
  }

  @PatchMapping("/{categoryId}")
  public EquipmentCategoryResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID categoryId,
      @Valid @RequestBody UpdateEquipmentCategoryRequest r) {
    access.requireManageEquipment(organizationId);
    return categories.update(organizationId, categoryId, r);
  }
}
