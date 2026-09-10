package org.civicops.equipment.checkout.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
import org.civicops.equipment.asset.EquipmentCondition;

public record CreateEquipmentCheckoutRequest(
    UUID borrowerUserId,
    @Size(max = 200) String borrowerName,
    @Email @Size(max = 320) String borrowerEmail,
    @NotNull Instant dueAt,
    @NotNull EquipmentCondition checkoutCondition,
    @Size(max = 10000) String notes) {}
