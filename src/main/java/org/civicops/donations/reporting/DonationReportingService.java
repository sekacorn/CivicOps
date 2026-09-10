package org.civicops.donations.reporting;

import java.math.*;
import java.time.LocalDate;
import java.util.*;
import org.civicops.donations.campaign.*;
import org.civicops.donations.donation.*;
import org.civicops.donations.donation.dto.DonationSummaryResponse;
import org.civicops.donations.donor.DonorRepository;
import org.civicops.donations.reporting.dto.*;
import org.civicops.shared.exception.*;
import org.civicops.shared.finance.Money;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonationReportingService {
  private final DonationRepository donations;
  private final DonationCampaignRepository campaigns;
  private final DonorRepository donors;

  public DonationReportingService(
      DonationRepository donations, DonationCampaignRepository campaigns, DonorRepository donors) {
    this.donations = donations;
    this.campaigns = campaigns;
    this.donors = donors;
  }

  @Transactional(readOnly = true)
  public DonationReportSummaryResponse summary(UUID org, LocalDate from, LocalDate to) {
    range(from, to);
    Object[] row =
        donations.summary(org, DonationStatus.RECORDED, lower(from), upper(to)).getFirst();
    long count = number(row[0]).longValue();
    BigDecimal total = money(row[1]), average = money(row[2]), restricted = money(row[3]);
    return new DonationReportSummaryResponse(
        count,
        total,
        average,
        restricted,
        money(total.subtract(restricted)),
        campaigns.countByOrganizationIdAndStatus(org, CampaignStatus.ACTIVE),
        number(row[4]).longValue());
  }

  @Transactional(readOnly = true)
  public List<PaymentMethodTotalResponse> byPayment(UUID org, LocalDate from, LocalDate to) {
    range(from, to);
    return donations.byPaymentMethod(org, DonationStatus.RECORDED, lower(from), upper(to)).stream()
        .map(
            r ->
                new PaymentMethodTotalResponse(
                    (DonationPaymentMethod) r[0], money(r[1]), number(r[2]).longValue()))
        .toList();
  }

  @Transactional(readOnly = true)
  public CampaignFinancialSummaryResponse campaign(UUID org, UUID id) {
    DonationCampaign c =
        campaigns
            .findByIdAndOrganizationId(id, org)
            .orElseThrow(() -> new ResourceNotFoundException("DonationCampaign", id));
    Object[] row = donations.campaignTotals(org, id, DonationStatus.RECORDED).getFirst();
    BigDecimal raised = money(row[0]), goal = money(c.getGoalAmount());
    BigDecimal remaining = goal.subtract(raised).max(BigDecimal.ZERO);
    return new CampaignFinancialSummaryResponse(
        id,
        c.getName(),
        goal,
        raised,
        money(remaining),
        Money.percent(raised, goal),
        number(row[1]).longValue());
  }

  @Transactional(readOnly = true)
  public List<CampaignFinancialSummaryResponse> byCampaign(UUID org) {
    return campaigns.findAllByOrganizationId(org).stream()
        .map(c -> campaign(org, c.getId()))
        .toList();
  }

  @Transactional(readOnly = true)
  public DonorHistoryResponse donorHistory(UUID org, UUID donorId, Pageable p) {
    if (donors.findByIdAndOrganizationId(donorId, org).isEmpty())
      throw new ResourceNotFoundException("Donor", donorId);
    Object[] row = donations.donorTotals(org, donorId, DonationStatus.RECORDED).getFirst();
    Specification<Donation> spec =
        (r, q, cb) ->
            cb.and(
                cb.equal(r.get("organization").get("id"), org),
                cb.equal(r.get("donor").get("id"), donorId));
    Page<DonationSummaryResponse> page =
        donations.findAll(spec, p).map(DonationSummaryResponse::from);
    return new DonorHistoryResponse(
        donorId, number(row[1]).longValue(), money(row[0]), (LocalDate) row[2], page);
  }

  private static void range(LocalDate from, LocalDate to) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Report end cannot precede start");
  }

  private static LocalDate lower(LocalDate value) {
    return value == null ? LocalDate.of(1, 1, 1) : value;
  }

  private static LocalDate upper(LocalDate value) {
    return value == null ? LocalDate.of(9999, 12, 31) : value;
  }

  private static Number number(Object x) {
    return x == null ? 0 : (Number) x;
  }

  private static BigDecimal money(Object x) {
    if (x == null) return Money.amount(BigDecimal.ZERO);
    return Money.amount(x instanceof BigDecimal value ? value : new BigDecimal(x.toString()));
  }
}
