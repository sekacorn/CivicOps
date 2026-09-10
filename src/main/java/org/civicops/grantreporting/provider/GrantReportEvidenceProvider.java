package org.civicops.grantreporting.provider;

import java.time.LocalDate;
import java.util.*;
import org.civicops.grantreporting.evidence.*;
import org.civicops.grants.grant.Grant;

public interface GrantReportEvidenceProvider {
  EvidenceSourceModule sourceModule();

  List<EvidenceDraft> collect(EvidenceContext context);

  record EvidenceContext(UUID organizationId, Grant grant, LocalDate from, LocalDate to) {}
}
