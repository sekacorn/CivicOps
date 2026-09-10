package org.civicops.grantreporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.civicops.grantreporting.evidence.*;
import org.civicops.grantreporting.narrative.DeterministicNarrativeProvider;
import org.civicops.grantreporting.report.GrantReport;
import org.civicops.grantreporting.report.GrantReportSection;
import org.junit.jupiter.api.Test;

class DeterministicNarrativeProviderTest {
  private final DeterministicNarrativeProvider provider = new DeterministicNarrativeProvider();

  @Test
  void narrativeUsesOnlyExactSupportedValuesAndInclusivePeriod() {
    GrantReport report = report();
    GrantReportSection section = section("Outcomes");
    var hours =
        evidence(
            "Approved volunteer hours",
            EvidenceSourceModule.VOLUNTEERS,
            new BigDecimal("120.50"),
            "hours",
            "Volunteer reporting aggregate");
    var events =
        evidence(
            "Completed events",
            EvidenceSourceModule.EVENTS,
            new BigDecimal("4"),
            "events",
            "Event reporting aggregate");
    var attendees =
        evidence(
            "Attendees",
            EvidenceSourceModule.EVENTS,
            new BigDecimal("180"),
            "attendees",
            "Event reporting aggregate");

    String narrative = provider.generate(report, section, List.of(hours, events, attendees));

    assertThat(narrative)
        .contains("2026-01-01 through 2026-06-30 (inclusive)")
        .contains("120.5 hours", "4 events", "180 attendees")
        .doesNotContain("approximately", "estimated", "181");
  }

  @Test
  void narrativeDistinguishesMissingZeroAndManualProvenance() {
    GrantReportEvidenceSnapshot missing = mock(GrantReportEvidenceSnapshot.class);
    when(missing.getMetricLabel()).thenReturn("Approved volunteer hours");
    when(missing.getValueState()).thenReturn(EvidenceValueState.MISSING);
    GrantReportEvidenceSnapshot zero =
        evidence(
            "Completed events",
            EvidenceSourceModule.EVENTS,
            BigDecimal.ZERO,
            "events",
            "Event reporting aggregate");
    GrantReportEvidenceSnapshot manual =
        evidence(
            "Community match",
            EvidenceSourceModule.MANUAL,
            new BigDecimal("2"),
            "partners",
            "Signed partner roster");

    String narrative =
        provider.generate(report(), section("Summary"), List.of(missing, zero, manual));

    assertThat(narrative)
        .contains("was unavailable", "was 0 events", "[MANUAL: Signed partner roster]");
  }

  private GrantReport report() {
    GrantReport report = mock(GrantReport.class);
    when(report.getPeriodStart()).thenReturn(LocalDate.of(2026, 1, 1));
    when(report.getPeriodEnd()).thenReturn(LocalDate.of(2026, 6, 30));
    return report;
  }

  private GrantReportSection section(String title) {
    GrantReportSection section = mock(GrantReportSection.class);
    when(section.getTitle()).thenReturn(title);
    return section;
  }

  private GrantReportEvidenceSnapshot evidence(
      String label, EvidenceSourceModule module, BigDecimal value, String unit, String reference) {
    GrantReportEvidenceSnapshot evidence = mock(GrantReportEvidenceSnapshot.class);
    when(evidence.getMetricLabel()).thenReturn(label);
    when(evidence.getSourceModule()).thenReturn(module);
    when(evidence.getValueState()).thenReturn(EvidenceValueState.VERIFIED);
    when(evidence.getNumericValue()).thenReturn(value);
    when(evidence.getUnit()).thenReturn(unit);
    when(evidence.getSourceReference()).thenReturn(reference);
    return evidence;
  }
}
