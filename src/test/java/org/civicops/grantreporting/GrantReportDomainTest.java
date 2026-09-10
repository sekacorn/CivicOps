package org.civicops.grantreporting;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.Set;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.grantreporting.evidence.EvidenceSourceModule;
import org.civicops.grantreporting.report.*;
import org.civicops.grantreporting.template.*;
import org.civicops.grants.grant.Grant;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class GrantReportDomainTest {
  @Test
  void reportingPeriodMustBeChronological() {
    assertThatThrownBy(() -> report(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 6, 30)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("precedes");
  }

  @Test
  void selectedSourcesAreStableAndManualCannotMasqueradeAsSystemSelection() {
    GrantReport report =
        report(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 6, 30),
            Set.of(
                EvidenceSourceModule.VOLUNTEERS,
                EvidenceSourceModule.GRANT,
                EvidenceSourceModule.MANUAL));
    assertThat(report.sources())
        .containsExactlyInAnyOrder(EvidenceSourceModule.GRANT, EvidenceSourceModule.VOLUNTEERS);
  }

  @Test
  void generatedEditedAndApprovedContentRemainDistinct() {
    GrantReport report = report(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
    GrantReportSection section = section(report, 100);
    Instant generatedAt = Instant.parse("2026-07-01T00:00:00Z");
    section.generate("System-generated content", "grant.total_spent=7500.00", generatedAt);
    report.generated(generatedAt);
    section.edit("Human-reviewed content");
    section.approve();
    assertThat(section.getGeneratedContent()).isEqualTo("System-generated content");
    assertThat(section.getEditedContent()).isEqualTo("Human-reviewed content");
    assertThat(section.getFinalContent()).isEqualTo("Human-reviewed content");
    assertThat(section.getEvidenceSummary()).contains("7500.00");
  }

  @Test
  void sectionMaximumLengthIsEnforced() {
    GrantReport report = report(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
    GrantReportSection section = section(report, 5);
    section.generate("draft", "evidence", Instant.now());
    assertThatThrownBy(() -> section.edit("sixsix"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("maximum");
  }

  @Test
  void finalizedReportRejectsRegenerationEditingAndUpdates() {
    GrantReport report = report(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
    GrantReportSection section = section(report, 100);
    Instant at = Instant.parse("2026-07-01T00:00:00Z");
    section.generate("Generated", "evidence", at);
    report.generated(at);
    section.approve();
    report.finalize(mock(User.class), at.plusSeconds(1));
    assertThatThrownBy(() -> report.generated(at.plusSeconds(2)))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> section.edit("changed")).isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> report.update(LocalDate.now(), null, null, null))
        .isInstanceOf(BusinessRuleException.class);
  }

  private GrantReport report(LocalDate from, LocalDate to) {
    return report(from, to, Set.of(EvidenceSourceModule.GRANT));
  }

  private GrantReport report(LocalDate from, LocalDate to, Set<EvidenceSourceModule> sources) {
    Organization organization = mock(Organization.class);
    Grant grant = mock(Grant.class);
    when(grant.getOrganization()).thenReturn(organization);
    return new GrantReport(
        grant, mock(GrantReportTemplate.class), from, to, sources, mock(User.class));
  }

  private GrantReportSection section(GrantReport report, int maxLength) {
    GrantReportTemplateSection template = mock(GrantReportTemplateSection.class);
    when(template.getSequenceNumber()).thenReturn(1);
    when(template.getTitle()).thenReturn("Executive Summary");
    when(template.getSectionType()).thenReturn(ReportSectionType.NARRATIVE);
    when(template.isRequired()).thenReturn(true);
    when(template.getMaxLength()).thenReturn(maxLength);
    return new GrantReportSection(report, template);
  }
}
