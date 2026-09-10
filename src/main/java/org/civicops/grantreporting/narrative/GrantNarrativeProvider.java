package org.civicops.grantreporting.narrative;

import java.util.List;
import org.civicops.grantreporting.evidence.GrantReportEvidenceSnapshot;
import org.civicops.grantreporting.report.*;

public interface GrantNarrativeProvider {
  String generate(
      GrantReport report, GrantReportSection section, List<GrantReportEvidenceSnapshot> evidence);
}
