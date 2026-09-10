package org.civicops.grantreporting.template;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrantReportTemplateSectionRepository
    extends JpaRepository<GrantReportTemplateSection, UUID> {
  Optional<GrantReportTemplateSection> findByIdAndOrganizationId(UUID id, UUID org);

  List<GrantReportTemplateSection> findAllByOrganizationIdAndTemplateIdOrderBySequenceNumber(
      UUID org, UUID template);

  boolean existsByTemplateIdAndSectionKeyIgnoreCase(UUID template, String key);

  boolean existsByTemplateIdAndSequenceNumber(UUID template, int sequence);
}
