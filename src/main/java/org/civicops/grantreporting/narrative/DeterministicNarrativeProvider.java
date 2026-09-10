package org.civicops.grantreporting.narrative;

import java.util.*;
import org.civicops.grantreporting.evidence.*;
import org.civicops.grantreporting.report.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "civicops.grant-report.narrative-provider",
    havingValue = "deterministic",
    matchIfMissing = true)
public class DeterministicNarrativeProvider implements GrantNarrativeProvider {
  public String generate(
      GrantReport report, GrantReportSection section, List<GrantReportEvidenceSnapshot> evidence) {
    StringBuilder out = new StringBuilder();
    out.append(section.getTitle())
        .append("\n\nReporting period: ")
        .append(report.getPeriodStart())
        .append(" through ")
        .append(report.getPeriodEnd())
        .append(" (inclusive).\n\n");
    for (var e : evidence) {
      if (e.getValueState() == EvidenceValueState.MISSING) {
        out.append(e.getMetricLabel()).append(" was unavailable for this reporting period.\n");
        continue;
      }
      if (e.getValueState() == EvidenceValueState.NOT_APPLICABLE) {
        out.append(e.getMetricLabel()).append(" was not applicable.\n");
        continue;
      }
      out.append(e.getMetricLabel()).append(" was ").append(value(e));
      if (e.getUnit() != null && !e.getUnit().equals("USD")) out.append(' ').append(e.getUnit());
      out.append(". [")
          .append(e.getSourceModule() == EvidenceSourceModule.MANUAL ? "MANUAL" : "SYSTEM")
          .append(": ")
          .append(e.getSourceReference())
          .append("]\n");
    }
    return out.toString().trim();
  }

  private static String value(GrantReportEvidenceSnapshot e) {
    if (e.getMonetaryValue() != null) return "$" + e.getMonetaryValue().toPlainString();
    if (e.getNumericValue() != null)
      return e.getNumericValue().stripTrailingZeros().toPlainString();
    return e.getTextValue();
  }
}
