package org.civicops.facilities.reporting;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.civicops.facilities.facility.FacilityRepository;
import org.civicops.facilities.reporting.dto.*;
import org.civicops.facilities.reservation.*;
import org.civicops.facilities.space.FacilitySpaceRepository;
import org.civicops.shared.exception.BusinessRuleException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacilityReportingService {
  private final FacilityRepository facilities;
  private final FacilitySpaceRepository spaces;
  private final FacilityReservationRepository reservations;
  private final Clock clock;

  public FacilityReportingService(
      FacilityRepository f, FacilitySpaceRepository s, FacilityReservationRepository r, Clock c) {
    facilities = f;
    spaces = s;
    reservations = r;
    clock = c;
  }

  @Transactional(readOnly = true)
  public FacilityReportSummaryResponse summary(UUID org, LocalDate from, LocalDate to) {
    dates(from, to);
    Instant a = start(from), b = end(to);
    return new FacilityReportSummaryResponse(
        org,
        from,
        to,
        facilities.countByOrganizationIdAndActiveTrue(org),
        spaces.countByOrganizationIdAndActiveTrueAndReservableTrue(org),
        reservations.countByOrganizationIdAndStatus(org, ReservationStatus.PENDING),
        reservations.countByOrganizationIdAndStatus(org, ReservationStatus.APPROVED),
        reservations.inPeriod(org, a, b),
        reservations.countByOrganizationIdAndStatus(org, ReservationStatus.CANCELLED));
  }

  @Transactional(readOnly = true)
  public FacilityUtilizationResponse utilization(UUID org, LocalDate from, LocalDate to) {
    dates(from, to);
    Instant a = start(from), b = end(to);
    Specification<FacilityReservation> s =
        (r, q, c) ->
            c.and(
                c.equal(r.get("organization").get("id"), org),
                c.lessThan(r.get("startDateTime"), b),
                c.greaterThan(r.get("endDateTime"), a));
    List<FacilityReservation> rows = reservations.findAll(s);
    var approved =
        rows.stream()
            .filter(
                x ->
                    x.getStatus() == ReservationStatus.APPROVED
                        || x.getStatus() == ReservationStatus.COMPLETED)
            .toList();
    BigDecimal total = hours(approved);
    var usage =
        approved.stream()
            .collect(Collectors.groupingBy(FacilityReservation::getFacilitySpace))
            .entrySet()
            .stream()
            .map(
                e ->
                    new FacilityUtilizationResponse.SpaceUsage(
                        e.getKey().getId(),
                        e.getKey().getName(),
                        e.getValue().size(),
                        hours(e.getValue())))
            .sorted(
                Comparator.comparing(FacilityUtilizationResponse.SpaceUsage::reservedHours)
                    .reversed())
            .limit(10)
            .toList();
    return new FacilityUtilizationResponse(
        org,
        from,
        to,
        rows.size(),
        total,
        rows.stream()
            .filter(
                x ->
                    x.getStatus() == ReservationStatus.APPROVED
                        || x.getStatus() == ReservationStatus.COMPLETED)
            .count(),
        count(rows, ReservationStatus.REJECTED),
        count(rows, ReservationStatus.CANCELLED),
        usage);
  }

  private static long count(List<FacilityReservation> r, ReservationStatus s) {
    return r.stream().filter(x -> x.getStatus() == s).count();
  }

  private static BigDecimal hours(Collection<FacilityReservation> r) {
    long seconds =
        r.stream()
            .mapToLong(x -> Duration.between(x.getStartDateTime(), x.getEndDateTime()).toSeconds())
            .sum();
    return BigDecimal.valueOf(seconds).divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
  }

  private Instant start(LocalDate d) {
    return d == null ? Instant.EPOCH : d.atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  private Instant end(LocalDate d) {
    return d == null ? Instant.now(clock) : d.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  private static void dates(LocalDate a, LocalDate b) {
    if (a != null && b != null && b.isBefore(a))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Report end precedes start");
  }
}
