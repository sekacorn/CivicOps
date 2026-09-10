package org.civicops.donations.donation;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DonationRepository
    extends JpaRepository<Donation, UUID>, JpaSpecificationExecutor<Donation> {
  Optional<Donation> findByIdAndOrganizationId(UUID id, UUID organizationId);

  boolean existsByOrganizationIdAndReferenceNumber(UUID organizationId, String referenceNumber);

  boolean existsByOrganizationIdAndReceiptNumber(UUID organizationId, String receiptNumber);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from Donation d where d.id=:id and d.organization.id=:organizationId")
  Optional<Donation> findLocked(@Param("organizationId") UUID organizationId, @Param("id") UUID id);

  @Query(
      "select coalesce(sum(d.amount),0),count(d) from Donation d where d.organization.id=:organizationId "
          + "and d.campaign.id=:campaignId and d.status=:status")
  List<Object[]> campaignTotals(
      @Param("organizationId") UUID organizationId,
      @Param("campaignId") UUID campaignId,
      @Param("status") DonationStatus status);

  @Query(
      "select count(d),coalesce(sum(d.amount),0),coalesce(avg(d.amount),0),"
          + "coalesce(sum(case when d.restricted=true then d.amount else 0 end),0),"
          + "count(distinct d.donor.id),max(d.donationDate) from Donation d where d.organization.id=:organizationId "
          + "and d.status=:status and d.donationDate>=:from and d.donationDate<=:to")
  List<Object[]> summary(
      @Param("organizationId") UUID organizationId,
      @Param("status") DonationStatus status,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  @Query(
      "select d.paymentMethod,sum(d.amount),count(d) from Donation d where d.organization.id=:organizationId "
          + "and d.status=:status and d.donationDate>=:from and d.donationDate<=:to "
          + "group by d.paymentMethod order by d.paymentMethod")
  List<Object[]> byPaymentMethod(
      @Param("organizationId") UUID organizationId,
      @Param("status") DonationStatus status,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  @Query(
      "select coalesce(sum(d.amount),0),count(d),max(d.donationDate) from Donation d where "
          + "d.organization.id=:organizationId and d.donor.id=:donorId and d.status=:status")
  List<Object[]> donorTotals(
      @Param("organizationId") UUID organizationId,
      @Param("donorId") UUID donorId,
      @Param("status") DonationStatus status);
}
