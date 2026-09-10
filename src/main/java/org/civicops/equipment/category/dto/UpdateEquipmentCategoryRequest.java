package org.civicops.equipment.category.dto;

import jakarta.validation.constraints.Size;

public record UpdateEquipmentCategoryRequest(
    @Size(max = 120) String name, @Size(max = 5000) String description, Boolean active) {}
