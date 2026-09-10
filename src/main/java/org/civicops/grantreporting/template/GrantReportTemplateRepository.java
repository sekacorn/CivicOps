package org.civicops.grantreporting.template;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface GrantReportTemplateRepository
    extends JpaRepository<GrantReportTemplate, UUID>,
        JpaSpecificationExecutor<GrantReportTemplate> {
  Optional<GrantReportTemplate> findByIdAndOrganizationId(UUID id, UUID org);
}
