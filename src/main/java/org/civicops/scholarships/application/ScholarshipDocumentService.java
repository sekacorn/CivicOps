package org.civicops.scholarships.application;

import java.time.*;
import java.util.*;
import org.civicops.core.user.UserService;
import org.civicops.scholarships.application.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScholarshipDocumentService {
  private final ScholarshipDocumentRepository documents;
  private final ScholarshipApplicationService applications;
  private final UserService users;
  private final Clock clock;

  public ScholarshipDocumentService(
      ScholarshipDocumentRepository d, ScholarshipApplicationService a, UserService u, Clock c) {
    documents = d;
    applications = a;
    users = u;
    clock = c;
  }

  @Transactional
  public ScholarshipDocumentResponse add(
      UUID org, UUID application, CreateScholarshipDocumentRequest r) {
    ScholarshipApplication a = applications.require(org, application);
    return ScholarshipDocumentResponse.from(
        documents.save(
            new ScholarshipDocument(
                a,
                r.documentType(),
                r.fileName().trim(),
                r.externalStorageReference().trim(),
                Instant.now(clock))));
  }

  @Transactional
  public ScholarshipDocumentResponse verify(UUID org, UUID application, UUID document, UUID actor) {
    applications.require(org, application);
    ScholarshipDocument d =
        documents
            .findById(document)
            .filter(
                x ->
                    x.getApplication().getId().equals(application)
                        && x.getApplication().getOrganization().getId().equals(org))
            .orElseThrow(
                () ->
                    new org.civicops.shared.exception.ResourceNotFoundException(
                        "Scholarship document", document));
    d.verify(users.requireEntity(actor));
    return ScholarshipDocumentResponse.from(d);
  }

  @Transactional(readOnly = true)
  public List<ScholarshipDocumentResponse> list(UUID org, UUID application) {
    applications.require(org, application);
    return documents.findAllByApplicationIdOrderByUploadedAtAsc(application).stream()
        .map(ScholarshipDocumentResponse::from)
        .toList();
  }
}
