package org.civicops.equipment.category.dto;

import jakarta.validation.constraints.*;

public record CreateEquipmentCategoryRequest(
    @NotBlank @Size(max = 120) String name, @Size(max = 5000) String description) {}
