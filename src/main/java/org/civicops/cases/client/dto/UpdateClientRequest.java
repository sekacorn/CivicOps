package org.civicops.cases.client.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UpdateClientRequest(
    @Size(max = 100) String externalReferenceNumber,
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Size(max = 100) String preferredName,
    @Past LocalDate dateOfBirth,
    @Email @Size(max = 320) String email,
    @Size(max = 50) String phone,
    @Size(max = 250) String addressLine1,
    @Size(max = 250) String addressLine2,
    @Size(max = 120) String city,
    @Size(max = 120) String state,
    @Size(max = 30) String postalCode,
    @Size(min = 2, max = 2) String country,
    @Size(max = 30) String preferredContactMethod,
    Boolean active) {}
