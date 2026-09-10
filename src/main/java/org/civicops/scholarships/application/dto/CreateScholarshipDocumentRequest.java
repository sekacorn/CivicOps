package org.civicops.scholarships.application.dto;

import jakarta.validation.constraints.*;
import org.civicops.scholarships.application.ScholarshipDocumentType;

public record CreateScholarshipDocumentRequest(
    @NotNull ScholarshipDocumentType documentType,
    @NotBlank @Size(max = 255) String fileName,
    @NotBlank @Size(max = 500) String externalStorageReference) {}
