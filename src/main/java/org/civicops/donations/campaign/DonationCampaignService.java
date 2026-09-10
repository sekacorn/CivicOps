package org.civicops.donations.campaign;

import java.time.LocalDate;
import java.util.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.UserService;
import org.civicops.donations.campaign.dto.*;
import org.civicops.shared.exception.*;
import org.civicops.shared.finance.Money;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonationCampaignService {
  private final DonationCampaignRepository campaigns;
  private final OrganizationService organizations;
  private final UserService users;

  public DonationCampaignService(
      DonationCampaignRepository campaigns, OrganizationService organizations, UserService users) {
    this.campaigns = campaigns;
    this.organizations = organizations;
    this.users = users;
  }

  @Transactional
  public CampaignResponse create(UUID org, UUID user, CreateCampaignRequest r) {
    validate(r.startDate(), r.endDate());
    return CampaignResponse.from(
        campaigns.save(
            new DonationCampaign(
                organizations.requireEntity(org),
                users.requireEntity(user),
                r.name().trim(),
                clean(r.description()),
                Money.amount(r.goalAmount()),
                r.startDate(),
                r.endDate())));
  }

  @Transactional(readOnly = true)
  public DonationCampaign require(UUID org, UUID id) {
    return campaigns
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("DonationCampaign", id));
  }

  @Transactional(readOnly = true)
  public CampaignResponse detail(UUID org, UUID id) {
    return CampaignResponse.from(require(org, id));
  }

  @Transactional
  public CampaignResponse update(UUID org, UUID id, UpdateCampaignRequest r) {
    DonationCampaign c = require(org, id);
    LocalDate start = r.startDate() == null ? c.getStartDate() : r.startDate();
    LocalDate end = r.endDate() == null ? c.getEndDate() : r.endDate();
    validate(start, end);
    c.update(
        trim(r.name()),
        clean(r.description()),
        r.goalAmount() == null ? null : Money.amount(r.goalAmount()),
        r.startDate(),
        r.endDate());
    return CampaignResponse.from(c);
  }

  @Transactional
  public CampaignResponse transition(UUID org, UUID id, CampaignStatus target) {
    DonationCampaign c = require(org, id);
    c.transition(target);
    return CampaignResponse.from(c);
  }

  @Transactional(readOnly = true)
  public Page<CampaignResponse> list(
      UUID org, CampaignStatus status, LocalDate from, LocalDate to, Pageable p) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException(
          "INVALID_DATE_RANGE", "Campaign filter end cannot precede start");
    Specification<DonationCampaign> s =
        (r, q, cb) -> cb.equal(r.get("organization").get("id"), org);
    if (status != null) s = s.and((r, q, cb) -> cb.equal(r.get("status"), status));
    if (from != null) s = s.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("startDate"), from));
    if (to != null) s = s.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("startDate"), to));
    return campaigns.findAll(s, p).map(CampaignResponse::from);
  }

  private static void validate(LocalDate start, LocalDate end) {
    if (start != null && end != null && end.isBefore(start))
      throw new BusinessRuleException(
          "INVALID_CAMPAIGN_DATE_RANGE", "Campaign end date cannot precede start date");
  }

  private static String trim(String x) {
    return x == null ? null : x.trim();
  }

  private static String clean(String x) {
    return x == null || x.isBlank() ? null : x.trim();
  }
}
