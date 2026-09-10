package org.civicops.facilities.availability.dto;

import jakarta.validation.constraints.NotNull;
import java.time.*;

public record OperatingHoursRequest(
    @NotNull DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {}
