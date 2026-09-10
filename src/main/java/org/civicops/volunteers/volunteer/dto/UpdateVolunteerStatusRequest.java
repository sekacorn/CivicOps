package org.civicops.volunteers.volunteer.dto;

import jakarta.validation.constraints.NotNull;
import org.civicops.volunteers.volunteer.VolunteerStatus;

public record UpdateVolunteerStatusRequest(@NotNull VolunteerStatus status) {}
