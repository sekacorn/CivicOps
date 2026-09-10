package org.civicops.equipment.maintenance.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.civicops.equipment.maintenance.MaintenanceType;

public record CreateMaintenanceRequest(
    @NotNull MaintenanceType maintenanceType,
    @NotBlank @Size(max = 10000) String description,
    Instant startedAt,
    @PositiveOrZero BigDecimal cost,
    @Size(max = 200) String vendor,
    @Size(max = 10000) String notes) {}
