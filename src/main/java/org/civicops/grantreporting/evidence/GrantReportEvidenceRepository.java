package org.civicops.grantreporting.evidence;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface GrantReportEvidenceRepository
    extends JpaRepository<GrantReportEvidenceSnapshot, UUID> {
  @Query(
      "select e from GrantReportEvidenceSnapshot e where e.organization.id=:org and e.report.id=:report order by e.sourceModule,e.metricKey,e.id")
  List<GrantReportEvidenceSnapshot> forReport(@Param("org") UUID org, @Param("report") UUID report);

  @Modifying
  @Query(
      "delete from GrantReportEvidenceSnapshot e where e.report.id=:report and e.sourceModule<>org.civicops.grantreporting.evidence.EvidenceSourceModule.MANUAL")
  void deleteSystemEvidence(@Param("report") UUID report);

  long countByReportId(UUID report);
}
