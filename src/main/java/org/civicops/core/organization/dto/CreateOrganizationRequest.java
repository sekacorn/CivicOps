package org.civicops.core.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.civicops.core.organization.OrganizationType;

public record CreateOrganizationRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 300) String legalName,
        @Size(max = 5000) String description,
        @NotNull OrganizationType organizationType,
        @Email @Size(max = 320) String email,
        @Size(max = 50) String phone,
        @Size(max = 500) String website,
        @Size(max = 250) String addressLine1,
        @Size(max = 250) String addressLine2,
        @Size(max = 120) String city,
        @Size(max = 120) String state,
        @Size(max = 30) String postalCode,
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "country must be a two-letter ISO code") String country) {
}
