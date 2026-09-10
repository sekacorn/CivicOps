package org.civicops.scholarships.application.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.scholarships.application.*;

public record ScholarshipDocumentResponse(
    UUID id,
    ScholarshipDocumentType documentType,
    String fileName,
    String externalStorageReference,
    Instant uploadedAt,
    boolean verified,
    UUID verifiedByUserId) {
  public static ScholarshipDocumentResponse from(ScholarshipDocument d) {
    return new ScholarshipDocumentResponse(
        d.getId(),
        d.getDocumentType(),
        d.getFileName(),
        d.getExternalStorageReference(),
        d.getUploadedAt(),
        d.isVerified(),
        d.getVerifiedBy() == null ? null : d.getVerifiedBy().getId());
  }
}
