package org.civicops.donations.campaign;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface DonationCampaignRepository
    extends JpaRepository<DonationCampaign, UUID>, JpaSpecificationExecutor<DonationCampaign> {
  Optional<DonationCampaign> findByIdAndOrganizationId(UUID id, UUID organizationId);

  List<DonationCampaign> findAllByOrganizationId(UUID organizationId);

  long countByOrganizationIdAndStatus(UUID organizationId, CampaignStatus status);
}
