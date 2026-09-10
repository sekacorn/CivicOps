package org.civicops.volunteers.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record VolunteerHoursTotal(UUID volunteerId, BigDecimal approvedHours) {}
