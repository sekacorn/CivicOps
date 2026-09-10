package org.civicops.scholarships.application;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "scholarship_document")
public class ScholarshipDocument extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "application_id", nullable = false)
  private ScholarshipApplication application;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ScholarshipDocumentType documentType;

  @Column(nullable = false, length = 255)
  private String fileName;

  @Column(nullable = false, length = 500)
  private String externalStorageReference;

  @Column(nullable = false)
  private Instant uploadedAt;

  @Column(nullable = false)
  private boolean verified;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "verified_by_user_id")
  private User verifiedBy;

  protected ScholarshipDocument() {}

  public ScholarshipDocument(
      ScholarshipApplication a,
      ScholarshipDocumentType type,
      String name,
      String reference,
      Instant at) {
    organization = a.getOrganization();
    application = a;
    documentType = type;
    fileName = name;
    externalStorageReference = reference;
    uploadedAt = at;
  }

  public void verify(User user) {
    verified = true;
    verifiedBy = user;
  }

  public ScholarshipApplication getApplication() {
    return application;
  }

  public ScholarshipDocumentType getDocumentType() {
    return documentType;
  }

  public String getFileName() {
    return fileName;
  }

  public String getExternalStorageReference() {
    return externalStorageReference;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }

  public boolean isVerified() {
    return verified;
  }

  public User getVerifiedBy() {
    return verifiedBy;
  }
}
