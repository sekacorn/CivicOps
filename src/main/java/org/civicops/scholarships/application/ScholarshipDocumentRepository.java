package org.civicops.scholarships.application;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScholarshipDocumentRepository extends JpaRepository<ScholarshipDocument, UUID> {
  List<ScholarshipDocument> findAllByApplicationIdOrderByUploadedAtAsc(UUID application);
}
