package org.civicops.grantreporting.report;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrantReportSectionRepository extends JpaRepository<GrantReportSection, UUID> {
  Optional<GrantReportSection> findByIdAndOrganizationId(UUID id, UUID org);

  List<GrantReportSection> findAllByOrganizationIdAndReportIdOrderBySequenceNumber(
      UUID org, UUID report);

  long countByReportIdAndRequiredTrueAndStatusNot(UUID report, ReportSectionStatus status);

  void deleteByReportId(UUID report);
}
