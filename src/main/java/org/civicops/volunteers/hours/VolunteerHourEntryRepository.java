package org.civicops.volunteers.hours;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface VolunteerHourEntryRepository
    extends JpaRepository<VolunteerHourEntry, UUID>, JpaSpecificationExecutor<VolunteerHourEntry> {
  Optional<VolunteerHourEntry> findByIdAndOrganizationId(UUID id, UUID orgId);

  Page<VolunteerHourEntry> findByOrganizationId(UUID orgId, Pageable pageable);

  Page<VolunteerHourEntry> findByOrganizationIdAndStatus(
      UUID orgId, HourEntryStatus status, Pageable pageable);

  Page<VolunteerHourEntry> findByOrganizationIdAndVolunteerId(
      UUID orgId, UUID volunteerId, Pageable pageable);

  @Query(
      "select coalesce(sum(h.hours),0) from VolunteerHourEntry h where h.organization.id=:orgId and"
          + " h.status=org.civicops.volunteers.hours.HourEntryStatus.APPROVED and h.serviceDate"
          + " between :from and :to")
  BigDecimal approvedTotal(
      @Param("orgId") UUID orgId, @Param("from") LocalDate from, @Param("to") LocalDate to);

  @Query(
      "select coalesce(sum(h.hours),0) from VolunteerHourEntry h where h.organization.id=:orgId and"
          + " h.volunteer.id=:volunteerId and"
          + " h.status=org.civicops.volunteers.hours.HourEntryStatus.APPROVED and h.serviceDate"
          + " between :from and :to")
  BigDecimal approvedTotalForVolunteer(
      @Param("orgId") UUID orgId,
      @Param("volunteerId") UUID volunteerId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  @Query(
      "select h.volunteer.id,coalesce(sum(h.hours),0) from VolunteerHourEntry h where"
          + " h.organization.id=:orgId and"
          + " h.status=org.civicops.volunteers.hours.HourEntryStatus.APPROVED and h.serviceDate"
          + " between :from and :to group by h.volunteer.id")
  List<Object[]> approvedTotalsByVolunteer(
      @Param("orgId") UUID orgId, @Param("from") LocalDate from, @Param("to") LocalDate to);

  @Query(
      "select coalesce(sum(h.hours),0) from VolunteerHourEntry h where h.organization.id=:orgId"
          + " and h.status=org.civicops.volunteers.hours.HourEntryStatus.APPROVED"
          + " and h.serviceDate between :from and :to and h.assignment is not null"
          + " and h.assignment.shift.opportunity.event.linkedGrant.id=:grantId")
  BigDecimal approvedTotalForGrantLinkedActivities(
      @Param("orgId") UUID orgId,
      @Param("grantId") UUID grantId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);
}
