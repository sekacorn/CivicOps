package org.civicops.donations.donation;

import java.time.LocalDate;
import java.util.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.UserService;
import org.civicops.donations.campaign.*;
import org.civicops.donations.donation.dto.*;
import org.civicops.donations.donor.*;
import org.civicops.shared.exception.*;
import org.civicops.shared.finance.Money;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonationService {
  private final DonationRepository donations;
  private final DonorRepository donors;
  private final DonationCampaignRepository campaigns;
  private final OrganizationService organizations;
  private final UserService users;

  public DonationService(
      DonationRepository donations,
      DonorRepository donors,
      DonationCampaignRepository campaigns,
      OrganizationService organizations,
      UserService users) {
    this.donations = donations;
    this.donors = donors;
    this.campaigns = campaigns;
    this.organizations = organizations;
    this.users = users;
  }

  @Transactional
  public DonationResponse create(UUID org, UUID user, CreateDonationRequest r) {
    validate(r);
    Donor donor = null;
    if (r.anonymous()) {
      if (r.donorId() != null)
        throw new BusinessRuleException(
            "ANONYMOUS_DONOR_ASSOCIATION_NOT_ALLOWED",
            "Anonymous donations must not retain a donor association");
    } else {
      if (r.donorId() == null)
        throw new BusinessRuleException(
            "DONOR_REQUIRED", "A non-anonymous donation requires a donor");
      donor =
          donors
              .findByIdAndOrganizationId(r.donorId(), org)
              .orElseThrow(() -> new ResourceNotFoundException("Donor", r.donorId()));
    }
    DonationCampaign campaign = null;
    if (r.campaignId() != null) {
      campaign =
          campaigns
              .findByIdAndOrganizationId(r.campaignId(), org)
              .orElseThrow(() -> new ResourceNotFoundException("DonationCampaign", r.campaignId()));
      if (campaign.getStatus() != CampaignStatus.ACTIVE)
        throw new BusinessRuleException(
            "CAMPAIGN_NOT_ACCEPTING_DONATIONS", "Only active campaigns accept donations");
    }
    String reference = clean(r.referenceNumber()), receipt = clean(r.receiptNumber());
    if (reference != null && donations.existsByOrganizationIdAndReferenceNumber(org, reference))
      throw duplicate("reference number");
    if (receipt != null && donations.existsByOrganizationIdAndReceiptNumber(org, receipt))
      throw duplicate("receipt number");
    AcknowledgementStatus acknowledgement =
        r.acknowledgementStatus() == null
            ? (r.anonymous() ? AcknowledgementStatus.NOT_REQUIRED : AcknowledgementStatus.PENDING)
            : r.acknowledgementStatus();
    Donation donation =
        new Donation(
            organizations.requireEntity(org),
            donor,
            campaign,
            r.anonymous(),
            Money.amount(r.amount()),
            r.donationDate(),
            r.paymentMethod(),
            clean(r.inKindDescription()),
            r.restricted(),
            clean(r.restrictionDescription()),
            clean(r.designation()),
            reference,
            receipt,
            acknowledgement,
            clean(r.notes()),
            users.requireEntity(user));
    try {
      return DonationResponse.from(donations.saveAndFlush(donation));
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException(
          "DUPLICATE_DONATION_IDENTIFIER",
          "Donation reference or receipt number already exists in the organization");
    }
  }

  @Transactional(readOnly = true)
  public Donation require(UUID org, UUID id) {
    return donations
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Donation", id));
  }

  @Transactional(readOnly = true)
  public DonationResponse detail(UUID org, UUID id) {
    return DonationResponse.from(require(org, id));
  }

  @Transactional
  public DonationResponse reverse(UUID org, UUID id, UUID user, String reason) {
    Donation d =
        donations
            .findLocked(org, id)
            .orElseThrow(() -> new ResourceNotFoundException("Donation", id));
    d.reverse(users.requireEntity(user), reason.trim());
    return DonationResponse.from(d);
  }

  @Transactional(readOnly = true)
  public Page<DonationSummaryResponse> list(
      UUID org,
      UUID donorId,
      UUID campaignId,
      DonationPaymentMethod method,
      Boolean restricted,
      LocalDate from,
      LocalDate to,
      Pageable p) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException(
          "INVALID_DATE_RANGE", "Donation filter end cannot precede start");
    Specification<Donation> s = (r, q, cb) -> cb.equal(r.get("organization").get("id"), org);
    if (donorId != null) s = s.and((r, q, cb) -> cb.equal(r.get("donor").get("id"), donorId));
    if (campaignId != null)
      s = s.and((r, q, cb) -> cb.equal(r.get("campaign").get("id"), campaignId));
    if (method != null) s = s.and((r, q, cb) -> cb.equal(r.get("paymentMethod"), method));
    if (restricted != null) s = s.and((r, q, cb) -> cb.equal(r.get("restricted"), restricted));
    if (from != null) s = s.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("donationDate"), from));
    if (to != null) s = s.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("donationDate"), to));
    return donations.findAll(s, p).map(DonationSummaryResponse::from);
  }

  private static void validate(CreateDonationRequest r) {
    if (r.paymentMethod() == DonationPaymentMethod.IN_KIND
        && (r.inKindDescription() == null || r.inKindDescription().isBlank()))
      throw new BusinessRuleException(
          "IN_KIND_DESCRIPTION_REQUIRED",
          "In-kind donations require a goods or services description");
    if (r.restricted() && blank(r.restrictionDescription()) && blank(r.designation()))
      throw new BusinessRuleException(
          "DONATION_RESTRICTION_REQUIRED",
          "Restricted donations require a restriction description or designation");
  }

  private static ConflictException duplicate(String field) {
    return new ConflictException(
        "DUPLICATE_DONATION_IDENTIFIER",
        "Donation " + field + " already exists in the organization");
  }

  private static boolean blank(String x) {
    return x == null || x.isBlank();
  }

  private static String clean(String x) {
    return blank(x) ? null : x.trim();
  }
}
